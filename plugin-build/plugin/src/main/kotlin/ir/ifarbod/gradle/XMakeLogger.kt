package ir.ifarbod.gradle

import org.gradle.api.logging.Logging

class XMakeLogger(
    private val extension: XMakePluginExtension,
) {
    private val logger = Logging.getLogger("xmake")

    fun verbose(message: String) {
        if (extension.logLevel == "verbose") {
            logger.lifecycle(message)
        }
    }

    fun verbose(
        tag: String,
        message: String,
    ) = verbose("[xmake/$tag]: $message")

    fun info(message: String) = logger.lifecycle(message)

    fun info(
        tag: String,
        message: String,
    ) = info("[xmake/$tag]: $message")

    fun error(message: String) = logger.error(message)

    fun error(
        tag: String,
        message: String,
    ) = error("[xmake/$tag]: $message")
}
