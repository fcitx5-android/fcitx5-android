/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.tasks.PrefabPackageConfigurationTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class NativeLibConventionPlugin : NativeBaseConventionPlugin() {

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply(target.libs.plugins.android.library.get().pluginId)

        target.extensions.configure<LibraryExtension> {
            packaging {
                jniLibs {
                    excludes.add("**/*.so")
                }
            }
            buildFeatures {
                prefab = true
                prefabPublishing = true
            }
        }

        target.tasks.withType<PrefabPackageConfigurationTask>().all {
            // The output of PrefabConfigurePackageTask is up-to-date even after running clean.
            // This is probably a bug of AGP. To work around, we need always rerun this task.
            doNotTrackState("The up-to-date checking of PrefabConfigurePackageTask is incorrect")
            // Native libraries must be built before we can properly configure prefab package,
            // otherwise it would produce empty IMPORTED_LOCATION in cmake config.
            runAfterNativeBuild(target)
        }
    }

}
