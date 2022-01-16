package me.rocka.fcitx5test.data

import android.util.Log
import me.rocka.fcitx5test.utils.Const
import me.rocka.fcitx5test.utils.appContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias SHA256 = String
typealias DataDescriptor = Pair<SHA256, Map<String, SHA256>>

object DataManager {

    sealed class Diff {
        abstract val key: String

        data class New(override val key: String, val new: String) : Diff()
        data class Update(override val key: String, val old: String, val new: String) : Diff()
        data class Delete(override val key: String, val old: String) : Diff()
    }

    private val dataDir = File(appContext.applicationInfo.dataDir)
    private val destDescriptorFile = File(dataDir, Const.dataDescriptorName)

    private val lock = ReentrantLock()

    // should be consistent with the deserialization in build.gradle.kts (:app)
    private fun deserialize(raw: String): Result<DataDescriptor> = runCatching {

        val jObject = JSONObject(raw)
        val sha256 = jObject.getString("sha256")
        val files = jObject.getJSONObject("files")
        val keys = files.names()!!
        val jArray = files.toJSONArray(keys)!!

        val map = mutableMapOf<String, String>()
        for (i in 0 until jArray.length()) {
            map[keys.getString(i)] = jArray.getString(i)
        }
        sha256 to map
    }

    private fun diff(old: DataDescriptor, new: DataDescriptor): List<Diff> =
        if (old.first == new.first)
            listOf()
        else
            new.second.mapNotNull {
                when {
                    it.key !in old.second -> Diff.New(it.key, it.value)
                    old.second[it.key] != it.value -> Diff.Update(
                        it.key,
                        old.second.getValue(it.key),
                        it.value
                    )
                    else -> null
                }
            }.toMutableList().apply {
                addAll(old.second.filterKeys { it !in new.second }
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
                ?: "" to mapOf()

        val bundledDescriptor =
            appContext.assets
                .open(Const.dataDescriptorName)
                .bufferedReader()
                .readText()
                .let { deserialize(it) }
                .getOrThrow()

        diff(destDescriptor, bundledDescriptor).forEach {
            Log.d(javaClass.name, it.toString())
            when (it) {
                is Diff.Delete -> deleteFileOrDir(it.key)
                is Diff.New -> copyFile(it.key)
                is Diff.Update -> copyFile(it.key)
            }
        }

        copyFile(Const.dataDescriptorName)

        Log.i(javaClass.name, "Synced!")
    }

    fun deleteAndSync() {
        dataDir.deleteRecursively()
        sync()
    }

    private fun deleteFileOrDir(path: String) = runCatching {
        val file = File(dataDir, path)
        if (file.isDirectory) {
            file.deleteRecursively()
        }
    }.getOrThrow()

    private fun copyFile(filename: String) = runCatching {
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
    }.getOrThrow()


}