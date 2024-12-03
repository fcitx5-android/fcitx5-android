/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.task
import java.io.File
import kotlin.io.path.isSymbolicLink

/**
 * Add task installFcitxConfig and installFcitxTranslation, using a random variant's cxx dir
 */
class FcitxComponentPlugin : Plugin<Project> {

    abstract class FcitxComponentExtension {
        var installLibraries: List<String> = emptyList()
        var excludeFiles: List<String> = emptyList()
        var modifyFiles: Map<String, (File) -> Unit> = emptyMap()
    }

    companion object {
        const val INSTALL_TASK = "installFcitxComponent"
        const val DELETE_TASK = "deleteFcitxComponentExcludeFiles"
        const val CLEAN_TASK = "cleanFcitxComponents"

        val DEPENDENT_TASKS = arrayOf(INSTALL_TASK, DELETE_TASK)
    }

    override fun apply(target: Project) {
        val installTask = target.task(INSTALL_TASK)
        val deleteTask = target.task(DELETE_TASK)
        registerCMakeTask(target, "generate-desktop-file", "config")
        registerCMakeTask(target, "translation-file", "translation")
        registerCleanTask(target)
        target.extensions.create<FcitxComponentExtension>("fcitxComponent")
        target.afterEvaluate {
            val ext = extensions.getByName<FcitxComponentExtension>("fcitxComponent")
            ext.installLibraries.forEach {
                val project = rootProject.project(":lib:$it")
                registerCMakeTask(target, "generate-desktop-file", "config", project)
                registerCMakeTask(target, "translation-file", "translation", project)
            }
            if (ext.excludeFiles.isNotEmpty()) {
                deleteTask.apply {
                    dependsOn(installTask)
                    doLast {
                        ext.excludeFiles.forEach {
                            project.assetsDir.resolve(it).delete()
                        }
                    }
                }
            }
        }
    }

    /**
     * build [sourceProject]'s cmake [target], and install its [component] to [project]'s assets
     */
    private fun registerCMakeTask(
        project: Project,
        target: String,
        component: String,
        sourceProject: Project = project
    ) {
        val taskName = if (project === sourceProject) {
            "installProject${component.capitalized()}"
        } else {
            "installLibrary${component.capitalized()}[${sourceProject.name}]"
        }
        val task = project.task(taskName) {
            runAfterNativeConfigure(sourceProject) { abiModel ->
                val cmake = abiModel.variant.module.cmake!!.cmakeExe!!
                sourceProject.exec {
                    workingDir = abiModel.cxxBuildFolder
                    commandLine(cmake, "--build", ".", "--target", target)
                }
                sourceProject.exec {
                    workingDir = abiModel.cxxBuildFolder
                    environment("DESTDIR", project.assetsDir.absolutePath)
                    commandLine(cmake, "--install", ".", "--component", component)
                }
                val ext = project.extensions.getByName<FcitxComponentExtension>("fcitxComponent")
                ext.modifyFiles.forEach { (path, function) ->
                    val file = project.assetsDir.resolve(path)
                    if (file.exists()) {
                        function.invoke(file)
                    }
                }
            }
        }
        project.tasks.getByName(INSTALL_TASK).dependsOn(task)
    }

    private fun registerCleanTask(project: Project) {
        project.task<Delete>(CLEAN_TASK) {
            delete(project.assetsDir.resolve("usr/share/locale"))
            // delete all non symlink files
            // true -> delete
            val files = mutableMapOf<String, Boolean>()
            project.assetsDir.resolve("usr/share/fcitx5").walkBottomUp()
                .onEnter {
                    // Don't enter symlink dir and don't delete
                    (!it.toPath().isSymbolicLink()).also { x -> files[it.path] = x }
                }
                .onLeave {
                    // Delete dir if all of its children can be deleted
                    files[it.path] =
                        it.listFiles()?.mapNotNull { f -> files[f.path] }?.all { x -> x } ?: false
                }
                .forEach {
                    // Don't delete symlink
                    files[it.path] = !it.toPath().isSymbolicLink()
                }
            files.forEach {
                if (it.value) {
                    logger.log(LogLevel.DEBUG, "Delete ${it.key}")
                    delete(it.key)
                }
            }
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
