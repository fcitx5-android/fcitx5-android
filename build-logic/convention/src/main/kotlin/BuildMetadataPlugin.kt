import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.InputChanges

class BuildMetadataPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.register<BuildMetadataTask>("generateBuildMetadata") {
                outputFile.set(file("build/outputs/apk/build-metadata.json"))
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
        fun execute(inputChanges: InputChanges) {
            with(project) {
                val metadata = BuildMetadata(buildVersionName, buildCommitHash, buildTimestamp)
                file.writeText(json.encodeToString(metadata))
            }
        }
    }
}