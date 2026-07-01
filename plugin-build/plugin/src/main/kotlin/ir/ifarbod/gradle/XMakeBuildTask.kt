package ir.ifarbod.gradle

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class XMakeBuildTask : XMakeTask() {
    init {
        group = XMAKE_GROUP
        description = "Build a configured build with XMake"
    }

    internal fun commandLine(): List<String> {
        val context = context()
        return buildList {
            addAll(listOf(context.program, "build"))
            addLogLevel(context.logLevel)
            addAll(context.targets)
        }
    }

    internal fun stageCommandLine(): List<String> {
        val context = context()
        val output = File(context.buildDirectory, "src/main/jniLibs/${context.buildArch}")
        return buildList {
            addAll(listOf(context.program, "install"))
            addLogLevel(context.logLevel, quietByDefault = true)
            addAll(listOf("-o", output.path))
            addAll(context.targets)
        }
    }

    @TaskAction
    fun build() {
        val context = taskContext ?: return
        requireProjectFile()
        XMakeExecutor(context.logger).exec(commandLine(), context.projectDirectory)
        XMakeExecutor(context.logger, showCommand = false).exec(stageCommandLine(), context.projectDirectory)
    }
}
