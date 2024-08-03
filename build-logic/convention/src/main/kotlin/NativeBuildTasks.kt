/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */
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
            abiModel = this@externalNativeBuild.abiModel()
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
