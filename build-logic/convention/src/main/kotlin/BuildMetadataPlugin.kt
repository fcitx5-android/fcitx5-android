/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Add task `generateBuildMetadata${Variant}`
 */
@Suppress("unused")
class BuildMetadataPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.configure<ApplicationExtension> {
            buildFeatures {
                buildConfig = true
            }
            defaultConfig {
                buildConfigField("String", "BUILD_GIT_HASH", "\"${target.buildCommitHash}\"")
                buildConfigField("long", "BUILD_TIME", target.buildTimestamp)
                buildConfigField("String", "DATA_DESCRIPTOR_NAME", "\"${DataDescriptorPlugin.FILE_NAME}\"")
            }
        }
        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val variantName = variant.name.capitalized()
                target.afterEvaluate {
                    target.tasks.register<BuildMetadataTask>("generateBuildMetadata${variantName}") {
                        val packageTask =
                            target.tasks.getByName("package${variantName}") as PackageAndroidArtifact
                        // create metadata file after package, because it's outputDirectory would
                        // be cleared at some time before package
                        mustRunAfter(packageTask)
                        val fileName = target.path.let {
                            // ":app" -> "" || ":plugin:anthy" -> ".plugin.anthy"
                            val suffix = if (it == ":app") "" else it.replace(':', '.')
                            "build-metadata${suffix}.json"
                        }
                        outputFile.set(packageTask.outputDirectory.file(fileName))
                    }.also {
                        target.tasks.getByName("assemble${variantName}").dependsOn(it)
                    }
                }
            }
        }
    }

    abstract class BuildMetadataTask : DefaultTask() {
        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        @TaskAction
        fun execute() {
            val jsonString = json.encodeToString(mapOf(
                "versionName" to project.buildVersionName,
                "commitHash" to project.buildCommitHash,
                "timestamp" to project.buildTimestamp
            ))
            outputFile.get().asFile.writeText(jsonString)
        }
    }
}