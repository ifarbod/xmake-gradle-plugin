package ir.ifarbod.gradle

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class XMakeRebuildTask : XMakeTask() {
    init {
        group = XMAKE_GROUP
        description = "Rebuild a configured build with XMake"
    }

    internal fun commandLine(): List<String> {
        val context = context()
        return buildList {
            addAll(listOf(context.program, "-r"))
            addLogLevel(context.logLevel)
            addAll(context.targets)
        }
    }

    internal fun stageCommandLine(): List<String> {
        val context = context()
        val output = File(context.buildDirectory, "libs/${context.buildArch}")
        return buildList {
            addAll(listOf(context.program, "install"))
            addLogLevel(context.logLevel, quietByDefault = true)
            addAll(listOf("-o", output.path))
            addAll(context.targets)
        }
    }

    @TaskAction
    fun rebuild() {
        val context = taskContext ?: return
        requireProjectFile()
        XMakeExecutor(context.logger).exec(commandLine(), context.projectDirectory)
        XMakeExecutor(context.logger, showCommand = false).exec(stageCommandLine(), context.projectDirectory)
    }
}
