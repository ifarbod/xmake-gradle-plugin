package ir.ifarbod.gradle

open class XMakePluginExtension {
    var program: String? = null
    var path: String? = null
    var ndk: String? = null
    var sdkver: Int? = null
    var stl: String? = null
    var stdcxx: Boolean? = null
    var buildDir: String? = null
    var buildMode: String? = null
    var logLevel: String? = null

    val arguments: MutableList<String> = mutableListOf()
    val cFlags: MutableList<String> = mutableListOf()
    val cppFlags: MutableList<String> = mutableListOf()
    val abiFilters: MutableSet<String> = linkedSetOf()
    val targets: MutableSet<String> = linkedSetOf()

    fun arguments(vararg values: String) {
        arguments.addAll(values)
    }

    fun setArguments(values: Collection<String>) {
        arguments.addAll(values)
    }

    fun cFlags(vararg values: String) {
        cFlags.addAll(values)
    }

    fun setCFlags(values: Collection<String>) {
        cFlags.addAll(values)
    }

    fun cppFlags(vararg values: String) {
        cppFlags.addAll(values)
    }

    fun setCppFlags(values: Collection<String>) {
        cppFlags.addAll(values)
    }

    fun abiFilters(vararg values: String) {
        abiFilters.addAll(values)
    }

    fun setAbiFilters(values: Collection<String>) {
        abiFilters.addAll(values)
    }

    fun targets(vararg values: String) {
        targets.addAll(values)
    }

    fun setTargets(values: Collection<String>) {
        targets.addAll(values)
    }
}
