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
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class AndroidBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(target.libs.plugins.kotlin.android.get().pluginId)

        target.extensions.configure(CommonExtension::class.java) {
            compileSdk = Versions.compileSdk
            buildToolsVersion = target.buildToolsVersion
            defaultConfig {
                minSdk = Versions.minSdk
            }
            compileOptions {
                sourceCompatibility = Versions.java
                targetCompatibility = Versions.java
            }
        }

        target.tasks.withType<KotlinCompile> {
            compilerOptions {
                // https://youtrack.jetbrains.com/issue/KT-55947
                jvmTarget.set(JvmTarget.fromTarget(Versions.java.toString()))
            }
        }

        target.extensions.configure<KotlinProjectExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.RequiresOptIn")
            }
        }
    }

}
