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

        target.extensions.configure<CommonExtension<*, *, *, *, *>>("android") {
            compileSdk = Versions.compileSdk
            buildToolsVersion = target.buildTools
            defaultConfig {
                minSdk = Versions.minSdk
            }
            compileOptions {
                sourceCompatibility = Versions.java
                targetCompatibility = Versions.java
            }
        }

        target.tasks.withType<KotlinCompile> {
            kotlinOptions {
                // https://youtrack.jetbrains.com/issue/KT-55947
                jvmTarget = Versions.java.toString()
                // https://issuetracker.google.com/issues/250197571
                // https://kotlinlang.org/docs/whatsnew1520.html#string-concatenation-via-invokedynamic
                freeCompilerArgs += "-Xstring-concat=inline"
            }
        }

        target.extensions.configure<KotlinProjectExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.RequiresOptIn")
            }
        }
    }

}
