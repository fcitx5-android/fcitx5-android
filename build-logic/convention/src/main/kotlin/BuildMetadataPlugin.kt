/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.tasks.PackageAndroidArtifact
import kotlinx.serialization.Serializable
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named

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
        val fileName = target.path.let {
            // ":app" -> "" || ":plugin:anthy" -> ".plugin.anthy"
            val suffix = if (it == ":app") "" else it.replace(':', '.')
            "build-metadata${suffix}.json"
        }
        val buildMetadataJson = json.encodeToString(BuildMetadata.fromProject(target))
        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val variantName = variant.name.capitalized()
                target.afterEvaluate {
                    target.tasks.named<PackageAndroidArtifact>("package$variantName") {
                        val buildMetadataFile = this.outputDirectory.file(fileName)
                        this.inputs.property("buildMetadataJson", buildMetadataJson)
                        this.outputs.file(buildMetadataFile).withPropertyName("buildMetadataFile")
                        doLast {
                            buildMetadataFile.get().asFile.writeText(buildMetadataJson)
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class BuildMetadata(
        val versionName: String,
        val commitHash: String,
        val timestamp: String
    ) {
        companion object {
            fun fromProject(p: Project) =
                BuildMetadata(p.buildVersionName, p.buildCommitHash, p.buildTimestamp)
        }
    }
}