package ir.ifarbod.gradle

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class XMakeInstallTask : XMakeTask() {
    init {
        group = XMAKE_GROUP
        description = "Install XMake artifacts into the Android jniLibs directory"
    }

    internal fun commandLine(script: File): List<String> {
        val context = context()
        return buildList {
            addAll(listOf(context.program, "lua"))
            addLogLevel(context.logLevel)
            add(script.absolutePath)
            addAll(listOf("-o", context.nativeLibsDirectory.absolutePath))
            addAll(listOf("-a", checkNotNull(context.buildArch)))
            addAll(context.targets)
        }
    }

    @TaskAction
    fun install() {
        val context = taskContext ?: return
        context.logger.info(">> install artifacts to ${context.nativeLibsDirectory.absolutePath}")
        XMakeExecutor(context.logger, showCommand = false)
            .exec(commandLine(extractInstallScript(context)), context.projectDirectory)
    }
}
