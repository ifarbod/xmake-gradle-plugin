package ir.ifarbod.gradle

import org.gradle.api.GradleException
import java.io.File
import java.io.IOException

class XMakeExecutor(
    private val logger: XMakeLogger,
    private val showCommand: Boolean = true,
) {
    fun exec(
        commandLine: List<String>,
        workingDirectory: File,
    ) {
        if (showCommand) {
            logger.info(">> ${commandLine.joinToString(" ")}")
        }

        workingDirectory.mkdirs()
        try {
            val process =
                ProcessBuilder(commandLine)
                    .directory(workingDirectory)
                    .redirectErrorStream(true)
                    .start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach(logger::info)
            }
            requireSuccess(process.waitFor())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GradleException("xmake command was interrupted", exception)
        } catch (exception: IOException) {
            throw GradleException("Could not execute xmake command", exception)
        }
    }

    private fun requireSuccess(exitCode: Int) {
        if (exitCode != 0) {
            throw GradleException("xmake command failed with exit code $exitCode")
        }
    }
}
