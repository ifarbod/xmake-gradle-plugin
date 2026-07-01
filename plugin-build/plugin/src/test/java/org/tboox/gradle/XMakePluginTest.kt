package ir.ifarbod.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class XMakePluginTest {
    @JvmField
    @Rule
    val testProjectDir = TemporaryFolder()

    @Test
    fun `plugin creates the xmake extension`() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(PLUGIN_ID)

        assertNotNull(project.extensions.findByType(XMakePluginExtension::class.java))
    }

    @Test
    fun `collection setters append to the correct options`() {
        val extension = XMakePluginExtension()

        extension.setArguments(listOf("--toolchain=clang"))
        extension.setCFlags(listOf("-DTEST"))
        extension.setCppFlags(listOf("-std=c++20"))
        extension.setAbiFilters(listOf("arm64-v8a"))
        extension.setTargets(listOf("native"))

        assertEquals(listOf("--toolchain=clang"), extension.arguments)
        assertEquals(listOf("-DTEST"), extension.cFlags)
        assertEquals(listOf("-std=c++20"), extension.cppFlags)
        assertEquals(setOf("arm64-v8a"), extension.abiFilters)
        assertEquals(setOf("native"), extension.targets)
    }

    @Test
    fun `configure command contains extension options`() {
        val project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
        val extension =
            XMakePluginExtension().apply {
                path = "jni/xmake.lua"
                program = "custom-xmake"
                buildMode = "release"
                logLevel = "debug"
                sdkver = 24
                stdcxx = false
                arguments("--toolchain=clang")
                cFlags("-DTEST")
                cppFlags("-std=c++20")
            }
        val task = project.tasks.register("configureXMake", XMakeConfigureTask::class.java).get()
        task.taskContext = XMakeTaskContext(extension, project, XMakeLogger(extension), "arm64-v8a")

        val commandLine = task.commandLine()

        assertEquals("custom-xmake", commandLine.first())
        assertTrue(commandLine.containsAll(listOf("-vD", "-a", "arm64-v8a", "-m", "release")))
        assertTrue(commandLine.contains("--toolchain=clang"))
        assertTrue(commandLine.contains("--cflags=-DTEST"))
        assertTrue(commandLine.contains("--cxxflags=-std=c++20"))
        assertTrue(commandLine.contains("--ndk_sdkver=24"))
        assertTrue(commandLine.contains("--ndk_stdcxx=n"))
    }

    @Test
    fun `configured plugin registers tasks for selected ABIs`() {
        File(testProjectDir.root, "settings.gradle").writeText("rootProject.name = 'test-project'")
        File(testProjectDir.root, "jni").mkdirs()
        File(testProjectDir.root, "jni/xmake.lua").writeText("target('native')\nset_kind('shared')")
        File(testProjectDir.root, "build.gradle").writeText(
            """
            plugins {
                id '$PLUGIN_ID'
            }
            xmake {
                path 'jni/xmake.lua'
                abiFilters 'arm64-v8a', 'x86_64'
                targets 'native'
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir.root)
                .withArguments("xmakeInstall", "--dry-run")
                .withPluginClasspath()
                .build()

        assertTrue(result.output.contains(":xmakeInstallForArm64 SKIPPED"))
        assertTrue(result.output.contains(":xmakeInstallForX64 SKIPPED"))
        assertTrue(!result.output.contains(":xmakeInstallForArmv7 SKIPPED"))
    }

    companion object {
        private const val PLUGIN_ID = "ir.ifarbod.xmake-gradle-plugin"
    }
}
