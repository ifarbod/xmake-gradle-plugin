package ir.ifarbod.gradle

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
open class XMakeConfigureTask : XMakeTask() {
    init {
        group = XMAKE_GROUP
        description = "Configure a build with XMake"
    }

    internal fun commandLine(): List<String> {
        val context = context()
        return buildList {
            add(context.program)
            addAll(listOf("f", "-c", "-y"))
            addLogLevel(context.logLevel)
            addAll(listOf("-p", "android"))
            context.buildArch?.let { addAll(listOf("-a", it)) }
            context.buildMode?.let { addAll(listOf("-m", it)) }
            addAll(context.arguments)
            if (context.cFlags.isNotEmpty()) {
                add("--cflags=${context.cFlags.joinToString(" ")}")
            }
            if (context.cppFlags.isNotEmpty()) {
                add("--cxxflags=${context.cppFlags.joinToString(" ")}")
            }
            context.ndkDirectory?.let { add("--ndk=${it.path}") }
            context.sdkVersion?.let { add("--ndk_sdkver=$it") }
            if (context.stdcxx == false) {
                add("--ndk_stdcxx=n")
            } else {
                context.stl?.let { add("--runtimes=$it") }
            }
            add("--builddir=${context.buildDirectory.path}")
        }
    }

    @TaskAction
    fun configure() {
        val context = requireProjectFile()
        XMakeExecutor(context.logger).exec(commandLine(), context.projectDirectory)
    }
}
