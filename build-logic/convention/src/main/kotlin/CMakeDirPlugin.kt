/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.internal.cxx.model.CxxAbiModel
import com.android.build.gradle.tasks.ExternalNativeBuildJsonTask
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withType
import java.io.File

val Project.cmakeDir: File
    get() {
        try {
            return extensions.extraProperties.get(CMakeDirPlugin.CMAKE_DIR) as File
        } catch (e: Exception) {
            error("Cannot find cmake dir. Did you apply org.fcitx.fcitx5.android.cmake-dir plugin and make your task `runAfterNativeConfigure`?")
        }
    }

/**
 * Important: make sure that the task runs after than the native task
 * Since we can't declare the dependency relationship, a weaker running order constraint must be enforced
 */

fun Task.runAfterNativeBuild(project: Project) {
    project.tasks.withType<ExternalNativeBuildTask>().whenTaskAdded {
        this@runAfterNativeBuild.mustRunAfter(this@whenTaskAdded)
    }
}

fun Task.runAfterNativeConfigure(project: Project) {
    project.tasks.withType<ExternalNativeBuildJsonTask>().whenTaskAdded {
        this@runAfterNativeConfigure.mustRunAfter(this@whenTaskAdded)
    }
}

/**
 * To obtain the cmake dir, the project should apply this plugin and call [Task.runAfterNativeConfigure]
 * in the task that accesses [cmakeDir]
 */
class CMakeDirPlugin : Plugin<Project> {

    companion object {
        const val CMAKE_DIR = "cmakeDir"
    }

    private fun Project.setCMakeDir(file: File) {
        project.extensions.extraProperties.set(CMAKE_DIR, file)
    }

    /**
     * Note *Graph*
     *
     * Installing fcitx components depends .cxx dir.
     * Since the native task `buildCMake$Variant\[$ABI]` depend on the current variant and ABI,
     * we should have registered installFcitxComponent tasks for the cartesian product of $Variant and $ABI, e.g. `installFcitxComponentDebug\[x86]`
     * However, this would be way more tedious, as the build variant and ABI actually do not affect components we are going to install.
     * The essential cause of this situation is that it's impossible for gradle to handle dynamic dependencies,
     * where we cannot add dependency when running a task. So a trick is used here: when the task graph
     * is evaluated, we look into it to find out the name of the native task which will be executed, and then store its output
     * path in global variable. This results in our tasks can not be executed directly without executing the dependent of the native task,
     * i.e. they are implicitly depending on the native task.
     */
    override fun apply(target: Project) {
        target.gradle.taskGraph.whenReady {
            val cmakeConfigureTask =
                target.tasks.filterIsInstance<ExternalNativeBuildJsonTask>().firstOrNull()
                    ?: return@whenReady
            val abiField =
                ExternalNativeBuildJsonTask::class.java.declaredFields.find { it.name == "abi" }
                    ?: return@whenReady
            abiField.isAccessible = true
            val abi = abiField.get(cmakeConfigureTask) as? CxxAbiModel ?: return@whenReady
            target.setCMakeDir(abi.cxxBuildFolder)
        }
    }

}
