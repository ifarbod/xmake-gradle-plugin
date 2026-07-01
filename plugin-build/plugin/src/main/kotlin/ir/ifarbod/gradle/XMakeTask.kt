package ir.ifarbod.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import java.io.File

@CacheableTask
open class XMakeTask : DefaultTask() {
    @get:Internal
    var taskContext: XMakeTaskContext? = null

    protected fun context(): XMakeTaskContext = checkNotNull(taskContext)

    protected fun requireProjectFile(): XMakeTaskContext =
        context().also {
            if (!it.projectFile.isFile) {
                throw GradleException("${it.projectFile.absolutePath} not found")
            }
        }

    protected fun MutableList<String>.addLogLevel(
        level: String,
        quietByDefault: Boolean = false,
    ) {
        when (level) {
            "verbose" -> add("-v")
            "debug" -> add("-vD")
            else -> if (quietByDefault) add("-q")
        }
    }

    protected fun extractInstallScript(context: XMakeTaskContext): File {
        val script = File(context.buildDirectory, "install_artifacts.lua")
        script.parentFile.mkdirs()
        val resource = javaClass.classLoader.getResourceAsStream(INSTALL_SCRIPT)
        if (resource == null) {
            throw GradleException("Plugin resource $INSTALL_SCRIPT is missing")
        }

        resource.use { input ->
            script.outputStream().use { out ->
                input.copyTo(out)
            }
        }
        return script
    }

    companion object {
        private const val INSTALL_SCRIPT = "lua/install_artifacts.lua"
    }
}
