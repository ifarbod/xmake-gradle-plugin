package ir.ifarbod.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File

class XMakeTaskContext(
    private val extension: XMakePluginExtension,
    private val project: Project,
    val logger: XMakeLogger,
    val buildArch: String? = null,
) {
    val program: String
        get() = extension.program ?: "xmake"

    val projectFile: File
        get() = project.file(checkNotNull(extension.path) { "xmake.path must be configured" })

    val projectDirectory: File
        get() = projectFile.parentFile

    val ndkDirectory: File?
        get() = extension.ndk?.let(::File)?.absoluteFile ?: androidProperty("ndkDirectory") as? File

    val sdkVersion: String?
        get() = extension.sdkver?.toString()

    val stdcxx: Boolean?
        get() = extension.stdcxx

    val stl: String?
        get() = extension.stl

    val buildDirectory: File
        get() =
            extension.buildDir?.let(project::file) ?: project.layout.buildDirectory
                .dir("xmake")
                .get()
                .asFile

    val nativeLibsDirectory: File
        get() = project.file("src/main/jniLibs")

    val cFlags: List<String>
        get() = extension.cFlags

    val cppFlags: List<String>
        get() = extension.cppFlags

    val arguments: List<String>
        get() = extension.arguments

    val targets: Set<String>
        get() = extension.targets

    val abiFilters: Set<String>
        get() = extension.abiFilters.ifEmpty { androidAbiFilters() }.ifEmpty { setOf(DEFAULT_ABI) }

    val logLevel: String
        get() = extension.logLevel ?: "normal"

    val buildMode: String?
        get() = extension.buildMode

    private fun androidAbiFilters(): Set<String> {
        val filters =
            project.extensions
                .findByName("android")
                ?.readProperty("defaultConfig")
                ?.readProperty("ndk")
                ?.readProperty("abiFilters")
        return when (filters) {
            is Provider<*> -> (filters.orNull as? Collection<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            is Collection<*> -> filters.filterIsInstance<String>().toSet()
            else -> emptySet()
        }
    }

    private fun androidProperty(name: String): Any? = project.extensions.findByName("android")?.readProperty(name)

    private fun Any.readProperty(name: String): Any? {
        val getterName = "get${name.replaceFirstChar(Char::uppercaseChar)}"
        return javaClass.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }?.invoke(this)
    }

    companion object {
        private const val DEFAULT_ABI = "armeabi-v7a"
    }
}
