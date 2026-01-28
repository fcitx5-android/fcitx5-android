/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class AndroidBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.configure<CommonExtension> {
            compileSdk = Versions.compileSdk
            buildToolsVersion = target.buildToolsVersion
            defaultConfig.apply {
                minSdk = Versions.minSdk
            }
            compileOptions.apply {
                sourceCompatibility = Versions.java
                targetCompatibility = Versions.java
            }
            lint.apply {
                disable += setOf("UseKtx")
            }
        }

        target.tasks.withType<KotlinCompile> {
            compilerOptions {
                // https://youtrack.jetbrains.com/issue/KT-55947
                jvmTarget.set(JvmTarget.fromTarget(Versions.java.toString()))
                // https://youtrack.jetbrains.com/issue/KT-73255/Change-defaulting-rule-for-annotations
                freeCompilerArgs.add("-Xannotation-default-target=param-property")
            }
        }

        target.extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                optIn.add("kotlin.RequiresOptIn")
            }
        }
    }

}
