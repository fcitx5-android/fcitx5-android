/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.create
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

    /**
     * symlinks to create after copying files
     * target -> source
     */
    val symlinks: MapProperty<String, String>
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
        extension.symlinks.convention(mapOf())
        target.task<DataDescriptorTask>(TASK) {
            inputDir.set(target.assetsDir)
            outputFile.set(target.assetsDir.resolve(FILE_NAME))
            excludes.set(extension.excludes)
            symlinks.set(extension.symlinks)
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
            val files: Map<String, String>,
            val symlinks: Map<String, String> = mapOf()
        )

        @get:Incremental
        @get:PathSensitive(PathSensitivity.NAME_ONLY)
        @get:InputDirectory
        abstract val inputDir: DirectoryProperty

        @get:Input
        abstract val excludes: ListProperty<String>

        @get:Input
        abstract val symlinks: MapProperty<String, String>

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        private val file by lazy { outputFile.get().asFile }


        private fun serialize(files: Map<String, String>, symlinks: Map<String, String>) {
            if (symlinks.keys.intersect(files.keys).isNotEmpty())
                throw IllegalArgumentException("Symlink target cannot be path in files")
            val descriptor = DataDescriptor(
                Hashing.sha256()
                    .hashString(
                        (files + symlinks).entries.joinToString { it.key + it.value },
                        Charset.defaultCharset()
                    ).toString(),
                files,
                symlinks
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
                val key = relativeFile.path.replace(File.separatorChar, '/')
                if (change.changeType == ChangeType.REMOVED || key in excludes.get()) {
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
            serialize(map.toSortedMap(), symlinks.get())
        }
    }
}