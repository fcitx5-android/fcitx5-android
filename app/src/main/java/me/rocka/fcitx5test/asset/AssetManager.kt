package me.rocka.fcitx5test.asset

import android.content.Context
import android.util.Log
import me.rocka.fcitx5test.Const
import me.rocka.fcitx5test.FcitxApplication
import me.rocka.fcitx5test.copyFile
import me.rocka.fcitx5test.deleteFileOrDir
import org.json.JSONObject
import java.io.File

object AssetManager {

    private val dataDir = File(context.applicationInfo.dataDir)
    private val destDescriptorFile = File(dataDir, Const.assetDescriptorName)

    private val context: Context
        get() = FcitxApplication.getInstance().applicationContext

    // should be consistent with the deserialization in build.gradle.kts (:app)
    private fun deserialize(raw: String): Result<AssetDescriptor> = runCatching {

        val jObject = JSONObject(raw)
        val sum = jObject.getInt("sum")
        val files = jObject.getJSONObject("files")
        val keys = files.names()!!
        val jArray = files.toJSONArray(keys)!!

        val map = mutableMapOf<String, String>()
        for (i in 0 until jArray.length()) {
            map[keys.getString(i)] = jArray.getString(i)
        }
        sum to map
    }

    private fun diff(old: AssetDescriptor, new: AssetDescriptor): List<Diff> =
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

    fun syncDataDir() {
        val destDescriptor =
            destDescriptorFile
                .takeIf { it.exists() && it.isFile }
                ?.runCatching { readText() }
                ?.getOrNull()
                ?.let { deserialize(it) }
                ?.getOrNull()
                ?: 0 to mapOf()

        val bundledDescriptor =
            context.assets
                .open(Const.assetDescriptorName)
                .bufferedReader()
                .readText()
                .let { deserialize(it) }
                .getOrThrow()

        diff(destDescriptor, bundledDescriptor).forEach {
            Log.d(javaClass.name, it.toString())
            when (it) {
                is Diff.Delete -> context.deleteFileOrDir(it.key)
                is Diff.New -> context.copyFile(it.key)
                is Diff.Update -> context.copyFile(it.key)
            }
        }

        context.copyFile(Const.assetDescriptorName)

        Log.i(javaClass.name, "Synced!")
    }

    fun cleanAndSync() {
        dataDir.deleteRecursively()
        syncDataDir()
    }


}