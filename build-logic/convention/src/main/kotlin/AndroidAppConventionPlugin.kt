/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import com.android.build.gradle.internal.tasks.ExpandArtProfileWildcardsTask
import com.android.build.gradle.internal.tasks.MergeArtProfileTask
import com.android.build.gradle.tasks.PackageApplication
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.AbstractProperty
import org.gradle.api.internal.provider.Providers
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * The prototype of an Android Application
 *
 * - Configure dependency for [DataDescriptorPlugin] task (If have)
 * - Provide default configuration for `android {...}`
 * - Add desugar JDK libs
 */
class AndroidAppConventionPlugin : AndroidBaseConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply(target.libs.plugins.android.application.get().pluginId)

        super.apply(target)

        target.extensions.configure<BaseAppModuleExtension> {
            defaultConfig {
                targetSdk = Versions.targetSdk
                versionCode = Versions.calculateVersionCode()
                versionName = target.buildVersionName
            }
            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    signingConfig = signingConfigs.fromProjectEnv(target)
                }
                debug {
                    applicationIdSuffix = ".debug"
                }
                all {
                    // remove META-INF/version-control-info.textproto
                    @Suppress("UnstableApiUsage")
                    vcsInfo.include = false
                }
            }
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        target.extensions.configure<ApplicationExtension> {
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
            packaging {
                resources {
                    excludes += setOf(
                        "/META-INF/*.version",
                        "/META-INF/*.kotlin_module",  // cannot be excluded actually
                        "/META-INF/androidx/**",
                        "/DebugProbesKt.bin",
                        "/kotlin-tooling-metadata.json"
                    )
                }
            }
        }

        // remove META-INF/com/android/build/gradle/app-metadata.properties
        target.tasks.withType<PackageApplication> {
            val valueField =
                AbstractProperty::class.java.declaredFields.find { it.name == "value" } ?: run {
                    println("class AbstractProperty field value not found, something could have gone wrong")
                    return@withType
                }
            valueField.isAccessible = true
            doFirst {
                valueField.set(appMetadata, Providers.notDefined<RegularFile>())
                allInputFilesWithNameOnlyPathSensitivity.removeAll { true }
            }
        }

        // try to remove <pkg_name>-<version_name>.kotlin_module, but it does not work ¯\_(ツ)_/¯
        target.tasks.withType<KotlinCompile> {
            doLast f@{
                val ktClass = outputs.files.files.filter { it.path.contains("kotlin-classes") }
                if (ktClass.isEmpty()) return@f
                val metaInf = ktClass.first().resolve("META-INF")
                if (!metaInf.exists() || !metaInf.isDirectory) return@f
                metaInf.listFiles()?.forEach {
                    if (it.name.endsWith(".kotlin_module")) {
                        it.delete()
                    }
                }
            }
        }

        // remove assets/dexopt/baseline.prof{,m} (baseline profile)
        target.tasks.withType<MergeArtProfileTask> { enabled = false }
        target.tasks.withType<ExpandArtProfileWildcardsTask> { enabled = false }
        target.tasks.withType<CompileArtProfileTask> { enabled = false }

        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            // Add dependency relationships for data descriptor task
            onVariants { v ->
                val variantName = v.name.capitalized()
                // Evaluation should be delayed as we need be able to see other tasks
                target.afterEvaluate {
                    tasks.findByName(DataDescriptorPlugin.TASK)?.also {
                        tasks.findByName("merge${variantName}Assets")?.dependsOn(it)
                        tasks.findByName("lintVitalAnalyzeRelease")?.dependsOn(it)
                        tasks.findByName("generateReleaseLintVitalReportModel")?.dependsOn(it)
                    }
                }
            }
            // Make data descriptor depend on fcitx component if have
            // Since we are using finalizeDsl, there is no need to do afterEvaluate
            finalizeDsl {
                target.tasks.findByName(DataDescriptorPlugin.TASK)?.also { dataDescriptorTask ->
                    FcitxComponentPlugin.DEPENDENT_TASKS
                        .mapNotNull { taskName -> target.tasks.findByName(taskName) }
                        .forEach { componentTask -> dataDescriptorTask.dependsOn(componentTask) }
                }
                // applicationId is not set upon apply
                it.defaultConfig {
                    target.setProperty("archivesBaseName", "$applicationId-$versionName")
                }
            }
        }

        target.pluginManager.apply(target.libs.plugins.aboutlibraries.get().pluginId)

        target.configure<AboutLibrariesExtension> {
            configPath = target.rootProject.relativePath(target.file("licenses"))
            excludeFields = arrayOf(
                "generated", "developers", "organization", "scm", "funding", "content"
            )
            fetchRemoteLicense = false
            fetchRemoteFunding = false
            includePlatform = false
        }

        target.dependencies.add("coreLibraryDesugaring", target.libs.android.desugarJDKLibs)
    }

}
