/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NativeAppConventionPlugin : NativeBaseConventionPlugin() {

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply("com.android.application")

        target.extensions.configure<BaseAppModuleExtension> {
            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
            buildFeatures {
                prefab = true
            }
        }
    }

}
