/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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

/**
 * Note: native build tasks would be called `:buildCMake$Variant[$ABI][$target1,$target2,etc]`
 * if `externalNativeBuild.cmake.targets` is provided with cmake target names, causing this method
 * to be inaccurate.
 * Consider using [Task.runAfterNativeConfigure] instead.
 */
fun Task.runAfterNativeBuild(project: Project) {
    mustRunAfter("${project.path}:buildCMakeDebug[${project.buildABI}]")
    mustRunAfter("${project.path}:buildCMakeRelWithDebInfo[${project.buildABI}]")
}

fun Task.runAfterNativeConfigure(project: Project) {
    mustRunAfter("${project.path}:configureCMakeDebug[${project.buildABI}]")
    mustRunAfter("${project.path}:configureCMakeRelWithDebInfo[${project.buildABI}]")
}

/**
 * To obtain the cmake dir, the project should apply this plugin and call [Task.runAfterNativeConfigure]
 * in the task that accesses [cmakeDir]
 */
class CMakeDirPlugin : Plugin<Project> {

    companion object {
        const val CMAKE_DIR = "cmakeDir"
    }

    private fun Project.setCmakeDir(file: File) {
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
            allTasks.find {
                it.project.name == target.name &&
                        (it.name.startsWith("configureCMakeDebug[") || it.name.startsWith("configureCMakeRelWithDebInfo["))
            }?.doLast {
                val buildModelFile = outputs.files.first().resolve("build_model.json")
                val buildModel = Json.parseToJsonElement(buildModelFile.readText()).jsonObject
                val cxxBuildFolder = buildModel["cxxBuildFolder"]!!.jsonPrimitive.content
                target.setCmakeDir(File(cxxBuildFolder))
            }
        }
    }

}
