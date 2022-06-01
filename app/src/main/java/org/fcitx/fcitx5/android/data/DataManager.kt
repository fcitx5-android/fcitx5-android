package org.fcitx.fcitx5.android.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias SHA256 = String

object DataManager {

    @Serializable
    data class DataDescriptor(
        val sha256: SHA256,
        val files: Map<String, SHA256>
    )

    sealed class Diff {
        abstract val key: String

        data class New(override val key: String, val new: String) : Diff()
        data class Update(override val key: String, val old: String, val new: String) : Diff()
        data class Delete(override val key: String, val old: String) : Diff()
    }

    val dataDir = File(appContext.applicationInfo.dataDir)
    private val destDescriptorFile = File(dataDir, Const.dataDescriptorName)

    private val lock = ReentrantLock()

    // should be consistent with the deserialization in build.gradle.kts (:app)
    private fun deserialize(raw: String) = runCatching {
        Json.decodeFromString<DataDescriptor>(raw)
    }

    private fun diff(old: DataDescriptor, new: DataDescriptor): List<Diff> =
        if (old.sha256 == new.sha256)
            listOf()
        else
            new.files.mapNotNull {
                when {
                    it.key !in old.files -> Diff.New(it.key, it.value)
                    old.files[it.key] != it.value -> Diff.Update(
                        it.key,
                        old.files.getValue(it.key),
                        it.value
                    )
                    else -> null
                }
            }.toMutableList().apply {
                addAll(old.files.filterKeys { it !in new.files }
                    .map { Diff.Delete(it.key, it.value) })
            }

    fun sync() = lock.withLock {
        val destDescriptor =
            destDescriptorFile
                .takeIf { it.exists() && it.isFile }
                ?.runCatching { readText() }
                ?.getOrNull()
                ?.let { deserialize(it) }
                ?.getOrNull()
                ?: DataDescriptor("", mapOf())

        val bundledDescriptor =
            appContext.assets
                .open(Const.dataDescriptorName)
                .bufferedReader()
                .use { it.readText() }
                .let { deserialize(it) }
                .getOrThrow()

        diff(destDescriptor, bundledDescriptor).forEach {
            Timber.d("Diff: $it")
            when (it) {
                is Diff.Delete -> deleteFileOrDir(it.key)
                is Diff.New -> copyFile(it.key)
                is Diff.Update -> copyFile(it.key)
            }
        }

        copyFile(Const.dataDescriptorName)

        Timber.i("Synced!")
    }

    fun deleteAndSync() {
        dataDir.deleteRecursively()
        sync()
    }

    private fun deleteFileOrDir(path: String) {
        val file = File(dataDir, path)
        if (file.isDirectory) {
            file.deleteRecursively()
        }
    }

    private fun copyFile(filename: String) {
        with(appContext.assets) {
            open(filename).use { i ->
                File(dataDir, filename)
                    .also { it.parentFile?.mkdirs() }
                    .outputStream().use { o ->
                        i.copyTo(o)
                        Unit
                    }
            }
        }
    }

}