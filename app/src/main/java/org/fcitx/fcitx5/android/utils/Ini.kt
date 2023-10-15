package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.core.RawConfig
import java.io.File

@JvmInline
value class Ini(val core: RawConfig) {

    val value: String
        get() = core.value

    fun get(vararg keys: String): Ini? {
        if (keys.isEmpty()) return null
        var current = core
        keys.forEach {
            current = current.findByName(it) ?: return null
        }
        return Ini(current)
    }

    fun set(vararg keys: String, raw: RawConfig) {
        var current = core
        keys.forEach {
            current = current.getOrCreate(it)
        }
        current.getOrCreate(raw.name).apply {
            // RawConfig's comment is immutable; is fine.
            value = raw.value
            subItems = raw.subItems
        }
    }

    fun set(vararg keys: String, str: String) {
        if (keys.isEmpty()) return
        var current = core
        keys.forEach {
            current = current.getOrCreate(it)
        }
        current.value = str
    }

    companion object {
        @JvmStatic
        private external fun readFromIni(src: String): RawConfig?

        @JvmStatic
        private external fun writeAsIni(dest: String, value: RawConfig)

        fun parseIniFromFile(file: File) = readFromIni(file.path)?.let { Ini(it) }

        fun writeIniToFile(ini: Ini, file: File) = writeAsIni(file.path, ini.core)
    }

}