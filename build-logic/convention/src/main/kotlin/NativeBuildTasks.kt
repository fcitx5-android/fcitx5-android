/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.internal.cxx.configure.rewriteWithLocations
import com.android.build.gradle.internal.cxx.logging.PassThroughRecordingLoggingEnvironment
import com.android.build.gradle.internal.cxx.model.CxxAbiModel
import com.android.build.gradle.tasks.ExternalNativeBuildJsonTask
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withType

fun ExternalNativeBuildJsonTask.abiModel(): CxxAbiModel {
    val abi = ExternalNativeBuildJsonTask::class.java.declaredFields.find { it.name == "abi" }!!
    abi.isAccessible = true
    return abi.get(this) as CxxAbiModel
}

/**
 * Important: make sure that the task runs after than the native task
 * Since we can't declare the dependency relationship, a weaker running order constraint must be enforced
 */

fun Task.runAfterNativeConfigure(project: Project, action: (abiModel: CxxAbiModel) -> Unit) {
    lateinit var abiModel: CxxAbiModel
    project.tasks.withType<ExternalNativeBuildJsonTask>().all externalNativeBuild@{
        this@runAfterNativeConfigure.mustRunAfter(this@externalNativeBuild)
        doFirst {
            // `CxxAbiModel.rewriteWithLocations` requires a "Non-default logger"
            // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/cxx/configure/NativeLocationsBuildService.kt;drc=b5516899015633c99dc64b510d9729c4e001e89c;l=67
            // just supply a random LoggingEnvironment or whatever this is
            PassThroughRecordingLoggingEnvironment().use {
                abiModel = this@externalNativeBuild.abiModel()
                    .rewriteWithLocations(nativeLocationsBuildService.get())
            }
        }
    }
    doLast {
        action.invoke(abiModel)
    }
}

fun Task.runAfterNativeBuild(project: Project) {
    project.tasks.withType<ExternalNativeBuildTask>().all externalNativeBuild@{
        this@runAfterNativeBuild.mustRunAfter(this@externalNativeBuild)
    }
}
