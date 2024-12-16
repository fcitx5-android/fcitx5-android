/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.tasks.PrefabPackageConfigurationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.withType

class FcitxDevelFilesPlugin  : Plugin<Project> {

    companion object {
        const val INSTALL_TASK = "installFcitxDevelFiles"
        const val CLEAN_TASK = "cleanFcitxDevelFiles"
    }

    private val Project.headersInstallDir
        get() = file("build/devel")

    override fun apply(target: Project) {
        registerInstallTask(target)
        registerCleanTask(target)
    }

    private fun registerInstallTask(project: Project) {
        val installDevelFilesTask = project.task(INSTALL_TASK) {
            runAfterNativeConfigure(project) { abiModel ->
                val cmake = abiModel.variant.module.cmake!!.cmakeExe!!
                project.exec {
                    workingDir = abiModel.cxxBuildFolder
                    environment("DESTDIR", project.headersInstallDir.absolutePath)
                    commandLine(cmake, "--install", ".", "--component", "Devel")
                }
            }
        }

        // Make sure devel files have been installed before configuring prefab package
        project.tasks.withType<PrefabPackageConfigurationTask>().all {
            dependsOn(installDevelFilesTask)
        }
    }

    private fun registerCleanTask(project: Project) {
        project.task<Delete>(CLEAN_TASK) {
            delete(project.headersInstallDir)
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
