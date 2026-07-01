package ir.ifarbod.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

const val XMAKE_EXTENSION = "xmake"
const val XMAKE_GROUP = "xmake"

class XMakePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(XMAKE_EXTENSION, XMakePluginExtension::class.java)
        project.afterEvaluate {
            activateIfConfigured(project, extension)
        }
    }

    private fun activateIfConfigured(
        project: Project,
        extension: XMakePluginExtension,
    ) {
        val path = extension.path ?: return
        if (!project.file(path).isFile) {
            return
        }

        val logger = XMakeLogger(extension)
        val projectContext = XMakeTaskContext(extension, project, logger)
        logger.info("plugin", "activated for project: ${project.name}")

        val architectureTasks =
            ARCHITECTURES.map { architecture ->
                registerArchitectureTasks(project, extension, logger, architecture)
            }
        registerAggregateTask<XMakeBuildTask>(
            project,
            "xmakeBuild",
            projectContext,
            architectureTasks.map { it.build },
        )
        registerAggregateTask<XMakeRebuildTask>(
            project,
            "xmakeRebuild",
            projectContext,
            architectureTasks.map { it.rebuild },
        )
        registerAggregateTask<XMakeInstallTask>(
            project,
            "xmakeInstall",
            projectContext,
            architectureTasks.map { it.install },
        )
        registerAggregateTask<XMakeCleanTask>(
            project,
            "xmakeClean",
            projectContext,
            architectureTasks.map { it.clean },
        )

        project.tasks.matching { it.name == "preBuild" }.configureEach { it.dependsOn("xmakeInstall") }
        project.tasks.matching { it.name == "clean" }.configureEach { it.dependsOn("xmakeClean") }
    }

    private fun registerArchitectureTasks(
        project: Project,
        extension: XMakePluginExtension,
        logger: XMakeLogger,
        architecture: Architecture,
    ): ArchitectureTasks {
        val context = XMakeTaskContext(extension, project, logger, architecture.abi)
        val configure =
            project.tasks.register("xmakeConfigureFor${architecture.taskSuffix}", XMakeConfigureTask::class.java) {
                it.taskContext = context
            }
        val build =
            registerDependentTask<XMakeBuildTask>(
                project,
                "xmakeBuildFor${architecture.taskSuffix}",
                configure,
                context,
            )
        val rebuild =
            registerDependentTask<XMakeRebuildTask>(
                project,
                "xmakeRebuildFor${architecture.taskSuffix}",
                configure,
                context,
            )
        val install =
            registerDependentTask<XMakeInstallTask>(
                project,
                "xmakeInstallFor${architecture.taskSuffix}",
                build,
                context,
            )
        val clean =
            registerDependentTask<XMakeCleanTask>(
                project,
                "xmakeCleanFor${architecture.taskSuffix}",
                configure,
                context,
            )
        return ArchitectureTasks(build, rebuild, install, clean)
    }

    private inline fun <reified T : XMakeTask> registerDependentTask(
        project: Project,
        name: String,
        dependency: TaskProvider<out Task>,
        context: XMakeTaskContext,
    ): TaskProvider<T> =
        project.tasks.register(name, T::class.java) {
            it.taskContext = context
            it.dependsOn(dependency)
        }

    private inline fun <reified T : XMakeTask> registerAggregateTask(
        project: Project,
        name: String,
        context: XMakeTaskContext,
        tasks: List<TaskProvider<T>>,
    ) {
        project.tasks.register(name, T::class.java) { aggregate ->
            val tasksByAbi: Map<String, TaskProvider<T>> =
                tasks.associateBy { task ->
                    task.name.substringAfter("For").toAbi()
                }

            context.abiFilters.forEach { abi ->
                val dependency: TaskProvider<T> =
                    tasksByAbi[abi]
                        ?: throw GradleException("invalid abiFilter: $abi")

                aggregate.dependsOn(dependency)
            }
        }
    }

    private fun String.toAbi(): String =
        ARCHITECTURES.firstOrNull { it.taskSuffix == this }?.abi
            ?: throw GradleException("Unknown XMake architecture: $this")

    private data class Architecture(
        val taskSuffix: String,
        val abi: String,
    )

    private data class ArchitectureTasks(
        val build: TaskProvider<XMakeBuildTask>,
        val rebuild: TaskProvider<XMakeRebuildTask>,
        val install: TaskProvider<XMakeInstallTask>,
        val clean: TaskProvider<XMakeCleanTask>,
    )

    companion object {
        private val ARCHITECTURES =
            listOf(
                Architecture("Arm64", "arm64-v8a"),
                Architecture("Armv7", "armeabi-v7a"),
                Architecture("Arm", "armeabi"),
                Architecture("RiscV64", "riscv64"),
                Architecture("X64", "x86_64"),
                Architecture("X86", "x86"),
            )
    }
}
