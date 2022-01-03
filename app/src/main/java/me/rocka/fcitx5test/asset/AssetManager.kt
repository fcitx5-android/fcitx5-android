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

    private fun deserialize(raw: String): Result<AssetDescriptor> = runCatching {

        val jObject = JSONObject(raw)

        val keys = jObject.names()!!
        val jArray = jObject.toJSONArray(jObject.names())!!

        val map = mutableMapOf<String, String>()
        for (i in 0 until jArray.length()) {
            map[keys.getString(i)] = jArray.getString(i)
        }
        map
    }

    private fun diff(old: AssetDescriptor, new: AssetDescriptor): List<Diff> =
        new.mapNotNull {
            when {
                it.key !in old -> Diff.New(it.key, it.value)
                old[it.key] != it.value -> Diff.Update(it.key, old.getValue(it.key), it.value)
                else -> null
            }
        }.toMutableList().apply {
            addAll(old.filterKeys { it !in new }.map { Diff.Delete(it.key, it.value) })
        }

    fun syncDataDir() {
        val destDescriptor =
            destDescriptorFile
                .takeIf { it.exists() && it.isFile }
                ?.runCatching { readText() }
                ?.getOrNull()
                ?.let { deserialize(it) }
                ?.getOrNull()
                ?: mapOf()
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

        Log.i(javaClass.name, "Synced!")
    }

    fun cleanAndSync() {
        dataDir.deleteRecursively()
        syncDataDir()
    }


}