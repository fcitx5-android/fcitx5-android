import com.android.build.api.dsl.ApplicationExtension
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
 * Add task generateBuildMetadata
 */
class BuildMetadataPlugin : Plugin<Project> {

    companion object {
        const val TASK = "generateBuildMetadata"
    }

    override fun apply(target: Project) {
        target.task<BuildMetadataTask>(TASK) {
            outputFile.set(target.file("build/outputs/apk/build-metadata.json"))
        }
        target.extensions.configure<ApplicationExtension> {
            defaultConfig {
                buildConfigField("String", "BUILD_GIT_HASH", "\"${target.buildCommitHash}\"")
                buildConfigField("long", "BUILD_TIME", target.buildTimestamp)
                buildConfigField("String", "DATA_DESCRIPTOR_NAME", "\"${dataDescriptorName}\"")
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