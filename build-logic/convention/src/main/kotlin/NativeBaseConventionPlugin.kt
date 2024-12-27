/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.task

open class NativeBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val prebuiltDir = target.rootProject.projectDir.resolve("lib/fcitx5/src/main/cpp/prebuilt")
        target.extensions.configure(CommonExtension::class.java) {
            ndkVersion = target.ndkVersion
            defaultConfig {
                minSdk = Versions.minSdk
                @Suppress("UnstableApiUsage")
                externalNativeBuild {
                    cmake {
                        arguments(
                            "-DANDROID_STL=c++_shared",
                            "-DANDROID_USE_LEGACY_TOOLCHAIN_FILE=OFF",
                            "-DVERSION_NAME=${Versions.baseVersionName}",
                            "-DPREBUILT_DIR=${prebuiltDir.absolutePath}"
                        )
                    }
                }
            }
            externalNativeBuild {
                cmake {
                    version = target.cmakeVersion
                    path("src/main/cpp/CMakeLists.txt")
                }
            }
            splits.abi {
                isEnable = true
                isUniversalApk = false
                reset()
                (target.buildAbiOverride?.split(",") ?: Versions.supportedABIs).forEach {
                    include(it)
                }
            }
        }
        registerCleanCxxTask(target)
    }

    private fun registerCleanCxxTask(project: Project) {
        project.task<Delete>("cleanCxxIntermediates") {
            delete(project.file(".cxx"))
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
