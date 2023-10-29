/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure

class NativeLibConventionPlugin : NativeBaseConventionPlugin() {

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply("com.android.library")

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
            libraryVariants.all {
                // The output of PrefabConfigurePackageTask is up-to-date even after running clean.
                // This is probably a bug of AGP. To work around, we need always rerun this task.
                target.tasks.named("prefab${name.capitalized()}ConfigurePackage").configure {
                    doNotTrackState("The up-to-date checking of PrefabConfigurePackageTask is incorrect")
                }
            }
        }
    }

}
