/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.task

/**
 * Add task `generateBuildMetadata${Variant}`
 */
class BuildMetadataPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.configure<BaseAppModuleExtension> {
            buildFeatures {
                buildConfig = true
            }
            defaultConfig {
                buildConfigField("String", "BUILD_GIT_HASH", "\"${target.buildCommitHash}\"")
                buildConfigField("long", "BUILD_TIME", target.buildTimestamp)
                buildConfigField("String", "DATA_DESCRIPTOR_NAME", "\"${DataDescriptorPlugin.FILE_NAME}\"")
            }
            applicationVariants.all {
                val variantName = name.capitalized()
                target.afterEvaluate {
                    target.task<BuildMetadataTask>("generateBuildMetadata${variantName}") {
                        val packageTask = packageApplicationProvider.get() // package${Variant} task
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
                        assembleProvider.get().dependsOn(it) // assemble${Variant} task
                    }
                }
            }
        }
    }

    abstract class BuildMetadataTask : DefaultTask() {
        @Serializable
        data class BuildMetadata(
            val versionName: String,
            val commitHash: String,
            val timestamp: String
        )

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        private val file by lazy { outputFile.get().asFile }

        @TaskAction
        fun execute() {
            with(project) {
                val metadata = BuildMetadata(buildVersionName, buildCommitHash, buildTimestamp)
                file.writeText(json.encodeToString(metadata))
            }
        }
    }
}