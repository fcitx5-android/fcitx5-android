/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration.FilterType
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NativeAppConventionPlugin : NativeBaseConventionPlugin() {

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply(target.libs.plugins.android.application.get().pluginId)

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

        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                // different version code based on abi
                variant.outputs.forEach { output ->
                    val abi = output.filters.first { it.filterType == FilterType.ABI }.identifier
                    output.versionCode.set(Versions.calculateVersionCode(abi))
                }
            }
        }
    }

}
