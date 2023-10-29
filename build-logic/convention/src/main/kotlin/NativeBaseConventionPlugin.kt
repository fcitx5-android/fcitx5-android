/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import Versions.cmakeVersion
import Versions.ndkVersion
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.task

open class NativeBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        @Suppress("UnstableApiUsage")
        target.extensions.configure<CommonExtension<*, *, *, *, *>>("android") {
            ndkVersion = target.ndkVersion
            defaultConfig {
                minSdk = Versions.minSdk
                externalNativeBuild {
                    cmake {
                        arguments(
                            "-DANDROID_STL=c++_shared",
                            "-DANDROID_USE_LEGACY_TOOLCHAIN_FILE=OFF"
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
            if (PlayRelease.run { target.buildPlayRelease }) {
                // in this case, the version code of arm64-v8a will be used for the single production,
                // unless `buildABI` is specified
                defaultConfig {
                    ndk {
                        abiFilters.add("armeabi-v7a")
                        abiFilters.add("arm64-v8a")
                        abiFilters.add("x86")
                        abiFilters.add("x86_64")
                    }
                }
            } else {
                splits {
                    abi {
                        isEnable = true
                        reset()
                        include(target.buildABI)
                        isUniversalApk = false
                    }
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
