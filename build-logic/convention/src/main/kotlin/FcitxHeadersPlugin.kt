/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.tasks.PrefabPackageConfigurationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.withType

class FcitxHeadersPlugin : Plugin<Project> {

    companion object {
        const val INSTALL_TASK = "installFcitxHeaders"
        const val CLEAN_TASK = "cleanFcitxHeaders"
    }

    private val Project.headersInstallDir
        get() = file("build/headers")

    override fun apply(target: Project) {
        registerInstallTask(target)
        registerCleanTask(target)
    }

    private fun registerInstallTask(project: Project) {
        val installHeadersTask = project.task(INSTALL_TASK) {
            runAfterNativeConfigure(project) { abiModel ->
                val cmake = abiModel.variant.module.cmake!!.cmakeExe!!
                project.exec {
                    workingDir = abiModel.cxxBuildFolder
                    environment("DESTDIR", project.headersInstallDir.absolutePath)
                    commandLine(cmake, "--install", ".", "--component", "header")
                }
            }
        }

        // Make sure headers have been installed before configuring prefab package
        project.tasks.withType<PrefabPackageConfigurationTask>().all {
            dependsOn(installHeadersTask)
        }

        project.extensions.configure<LibraryAndroidComponentsExtension> {
            finalizeDsl {
                @Suppress("UnstableApiUsage")
                it.prefab.forEach { library ->
                    library.headers?.let { path -> project.file(path).mkdirs() }
                }
            }
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
