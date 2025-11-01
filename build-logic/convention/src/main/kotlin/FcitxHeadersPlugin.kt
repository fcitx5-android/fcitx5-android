/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.internal.cxx.model.CxxAbiModel
import com.android.build.gradle.tasks.ExternalNativeBuildJsonTask
import com.android.build.gradle.tasks.PrefabPackageConfigurationTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

@Suppress("unused")
class FcitxHeadersPlugin : Plugin<Project> {

    abstract class FcitxHeadersExtension {
        var installDevelComponent: Boolean = false
    }

    companion object {
        const val HEADERS_TASK = "installFcitxHeaders"
        const val DEVEL_TASK = "installFcitxDevelComponent"
        const val CLEAN_TASK = "cleanFcitxHeaders"
        const val EXTENSION = "fcitxHeaders"
    }

    private val Project.headersInstallDir
        get() = file("build/headers")

    private val Project.develComponentInstallDir
        get() = file("build/devel")

    override fun apply(target: Project) {
        // mkdir for all prefab library headers
        target.extensions.configure<LibraryAndroidComponentsExtension> {
            finalizeDsl {
                @Suppress("UnstableApiUsage")
                it.prefab.forEach { library ->
                    library.headers?.let { path -> target.file(path).mkdirs() }
                }
            }
        }
        registerInstallTask(target, HEADERS_TASK, "header", target.headersInstallDir)
        registerCleanTask(target)
        target.extensions.create<FcitxHeadersExtension>(EXTENSION)
        target.afterEvaluate {
            val ext = extensions.getByName<FcitxHeadersExtension>(EXTENSION)
            if (ext.installDevelComponent) {
                registerInstallTask(target, DEVEL_TASK, "Devel", target.develComponentInstallDir)
            }
        }
    }

    abstract class InstallHeaderTask : DefaultTask() {
        @get:Input
        abstract val component: Property<String>

        @get:Input
        abstract val dest: Property<File>

        @get:Input
        @get:Optional
        abstract val abiModel: Property<CxxAbiModel>

        @TaskAction
        fun execute() {
            val component = this.component.get()
            val dest = this.dest.get()
            val abiModel = abiModel.get()
            val cmake = abiModel.variant.module.cmake!!.cmakeExe!!
            project.providers.exec {
                workingDir = abiModel.cxxBuildFolder
                environment("DESTDIR", dest.absolutePath)
                commandLine(cmake, "--install", ".", "--component", component)
            }.result.get()
        }
    }

    private fun registerInstallTask(project: Project, name: String, component: String, dest: File) {
        val cxxAbiModel = project.getCxxAbiModelProperty()
        val installHeadersTask = project.tasks.register<InstallHeaderTask>(name) {
            this.component.set(component)
            this.dest.set(dest)
            this.abiModel.set(cxxAbiModel)
            this.mustRunAfter(project.tasks.withType<ExternalNativeBuildJsonTask>())
        }
        // Make sure headers have been installed before configuring prefab package
        project.tasks.withType<PrefabPackageConfigurationTask>().all {
            dependsOn(installHeadersTask)
        }
    }

    private fun registerCleanTask(project: Project) {
        project.tasks.register<Delete>(CLEAN_TASK) {
            delete(project.headersInstallDir)
            delete(project.develComponentInstallDir)
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
