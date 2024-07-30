/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import Versions.buildTools
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class AndroidBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.android")

        target.extensions.configure(CommonExtension::class.java) {
            compileSdk = Versions.compileSdk
            buildToolsVersion = target.buildTools
            defaultConfig {
                minSdk = Versions.minSdk
            }
            compileOptions {
                sourceCompatibility = Versions.java
                targetCompatibility = Versions.java
            }
            buildTypes {
                onEach {
                    // remove META-INF/version-control-info.textproto
                    @Suppress("UnstableApiUsage")
                    it.vcsInfo.include = false
                }
            }
        }

        target.tasks.withType<KotlinCompile> {
            kotlinOptions {
                // https://youtrack.jetbrains.com/issue/KT-55947
                jvmTarget = Versions.java.toString()
            }
        }

        target.extensions.configure<KotlinProjectExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.RequiresOptIn")
            }
        }

        target.afterEvaluate {
            // remove assets/dexopt/baseline.prof{,m} (baseline profile)
            target.tasks.findByName("prepareReleaseArtProfile")?.apply {
                enabled = false
            }
            target.tasks.findByName("mergeReleaseArtProfile")?.apply {
                enabled = false
            }
            target.tasks.findByName("expandReleaseL8ArtProfileWildcards")?.apply {
                enabled = false
            }
            target.tasks.findByName("expandReleaseArtProfileWildcards")?.apply {
                enabled = false
            }
            target.tasks.findByName("compileReleaseArtProfile")?.apply {
                enabled = false
            }
            target.tasks.findByName("writeReleaseAppMetadata")?.apply {
                enabled = false
            }
        }
    }

}
