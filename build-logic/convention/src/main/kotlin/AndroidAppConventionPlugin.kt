/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.tasks.PackageApplication
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.Providers
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.lang.reflect.Field

/**
 * The prototype of an Android Application
 *
 * - Configure dependency for [DataDescriptorPlugin] task (If have)
 * - Provide default configuration for `android {...}`
 * - Add desugar JDK libs
 */
class AndroidAppConventionPlugin : AndroidBaseConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")

        super.apply(target)

        target.extensions.configure<BaseAppModuleExtension> {
            defaultConfig {
                targetSdk = Versions.targetSdk
                versionCode = Versions.calculateVersionCode(target.buildABI)
                versionName = target.buildVersionName
            }
            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    // config singing key for play release
                    signingConfig = with(PlayRelease) {
                        if (target.buildPlayRelease) {
                            signingConfigs.create("playRelease") {
                                storeFile = File(target.storeFile!!)
                                storePassword = target.storePassword
                                keyAlias = target.keyAlias
                                keyPassword = target.keyPassword
                            }
                        } else null
                    }
                }
                debug {
                    applicationIdSuffix = ".debug"
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
                        "/kotlin/**",
                        "/kotlin-tooling-metadata.json"
                    )
                }
            }
        }

        // remove META-INF/com/android/build/gradle/app-metadata.properties
        target.tasks.withType<PackageApplication> {
            var javaClass: Class<*>? = appMetadata.javaClass
            var valueField: Field? = null
            while (javaClass != null) {
                valueField = javaClass.declaredFields.find { it.name == "value" }
                if (valueField != null) break
                else javaClass = javaClass.superclass
            }
            valueField?.isAccessible = true
            var appMetadataPath: String? = null
            target.afterEvaluate {
                // writeReleaseAppMetadata was skipped... but why?
                if (appMetadata.isPresent) {
                    appMetadata.asFile.get().apply {
                        appMetadataPath = path
                        parentFile.mkdirs()
                        // make sure appMetadata file exists before the task
                        writeText("")
                    }
                }
            }
            doFirst {
                appMetadataPath?.let { File(it).delete() }
                valueField?.set(appMetadata, Providers.notDefined<RegularFile>())
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
                        println("deleting ${it.path}")
                        it.delete()
                    }
                }
            }
        }

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
                target.tasks.findByName(FcitxComponentPlugin.INSTALL_TASK)?.also { componentTask ->
                    target.tasks.findByName(DataDescriptorPlugin.TASK)?.dependsOn(componentTask)
                }
                // applicationId is not set upon apply
                it.defaultConfig {
                    target.setProperty("archivesBaseName", "$applicationId-$versionName")
                }
            }
        }

        runCatching {
            target.pluginManager.apply(
                target.versionCatalog.findPlugin("aboutlibraries").get().get().pluginId
            )
        }.onFailure {
            it.printStackTrace()
        }

        target.configure<AboutLibrariesExtension> {
            excludeFields = arrayOf(
                "generated", "developers", "organization", "scm", "funding", "content"
            )
            fetchRemoteLicense = false
            fetchRemoteFunding = false
            includePlatform = false
        }

        runCatching {
            target.dependencies.add(
                "coreLibraryDesugaring",
                target.versionCatalog.findLibrary("android.desugarJDKLibs").get()
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

}
