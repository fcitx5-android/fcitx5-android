/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import Versions.cmakeVersion
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import java.io.File

val Project.androidSdkPath: File
    get() = extensions.extraProperties.get(AndroidSdkPathPlugin.ANDROID_SDK_PATH) as? File
        ?: error("Cannot find Android SDK path. Did you apply org.fcitx.fcitx5.android.android-sdk-path plugin?")

val Project.cmakeBinary: String
    get() = androidSdkPath.resolve("cmake/$cmakeVersion/bin/cmake").absolutePath

class AndroidSdkPathPlugin : Plugin<Project> {

    companion object {
        const val ANDROID_SDK_PATH = "AndroidSdkPath"
    }

    private fun Project.setSdkPath(file: File) {
        project.extensions.extraProperties.set(ANDROID_SDK_PATH, file)
    }

    override fun apply(target: Project) {
        val androidComponents =
            target.extensions.getByName<AndroidComponentsExtension<*, *, *>>("androidComponents")
        val sdkPath = androidComponents.sdkComponents.sdkDirectory.get().asFile
        target.setSdkPath(sdkPath)
    }

}
