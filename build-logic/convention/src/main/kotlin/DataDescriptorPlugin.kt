import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.task
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.com.google.common.hash.Hashing
import org.jetbrains.kotlin.com.google.common.io.ByteSource
import java.io.File
import java.nio.charset.Charset
import kotlin.collections.set

interface DataDescriptorPluginExtension {
    /**
     * paths relative to asset dir to be excluded
     */
    val excludes: ListProperty<String>
}

/**
 * Add task generateDataDescriptor
 */
class DataDescriptorPlugin : Plugin<Project> {

    companion object {
        const val TASK = "generateDataDescriptor"
        const val CLEAN_TASK = "cleanDataDescriptor"
        const val FILE_NAME = "descriptor.json"
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create<DataDescriptorPluginExtension>(TASK)
        extension.excludes.convention(listOf())
        target.task<DataDescriptorTask>(TASK) {
            inputDir.set(target.assetsDir)
            outputFile.set(target.assetsDir.resolve(FILE_NAME))
        }
        target.task<Delete>(CLEAN_TASK) {
            delete(target.assetsDir.resolve(FILE_NAME))
        }.also {
            target.cleanTask.dependsOn(it)
        }
    }

    abstract class DataDescriptorTask : DefaultTask() {
        @Serializable
        data class DataDescriptor(
            val sha256: String,
            val files: Map<String, String>
        )

        @get:Incremental
        @get:PathSensitive(PathSensitivity.NAME_ONLY)
        @get:InputDirectory
        abstract val inputDir: DirectoryProperty

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        private val file by lazy { outputFile.get().asFile }

        private val excludes by lazy {
            project.extensions.getByName<DataDescriptorPluginExtension>(TASK).excludes.get()
        }

        private fun serialize(map: Map<String, String>) {
            file.deleteOnExit()
            val descriptor = DataDescriptor(
                Hashing.sha256()
                    .hashString(
                        map.entries.joinToString { it.key + it.value },
                        Charset.defaultCharset()
                    ).toString(),
                map
            )
            file.writeText(json.encodeToString(descriptor))
        }

        private fun deserialize(): Map<String, String> =
            json.decodeFromString<DataDescriptor>(file.readText()).files

        companion object {
            fun sha256(file: File): String =
                ByteSource.wrap(file.readBytes()).hash(Hashing.sha256()).toString()
        }

        @TaskAction
        fun execute(inputChanges: InputChanges) {
            val map =
                file.exists()
                    .takeIf { it }
                    ?.runCatching {
                        deserialize()
                            // remove all old dirs
                            .filterValues { it.isNotBlank() }
                            .toMutableMap()
                    }
                    ?.getOrNull()
                    ?: mutableMapOf()

            fun File.allParents(): List<File> =
                if (parentFile == null || parentFile.path in map)
                    listOf()
                else
                    listOf(parentFile) + parentFile.allParents()
            inputChanges.getFileChanges(inputDir).forEach { change ->
                if (change.file.name == file.name)
                    return@forEach
                logger.log(LogLevel.DEBUG, "${change.changeType}: ${change.normalizedPath}")
                val relativeFile = change.file.relativeTo(file.parentFile)
                val key = relativeFile.path
                if (change.changeType == ChangeType.REMOVED || key in excludes) {
                    map.remove(key)
                } else {
                    map[key] = sha256(change.file)
                }
            }
            // calculate dirs
            inputDir.asFileTree.forEach {
                it.relativeTo(file.parentFile).allParents().forEach { p ->
                    map[p.path] = ""
                }
            }
            serialize(map.toSortedMap())
        }
    }
}