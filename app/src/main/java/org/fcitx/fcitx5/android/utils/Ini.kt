package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.core.RawConfig
import java.io.File


@JvmInline
value class Ini(val core: RawConfig) {

    val value
        get() = core.value

    fun get(vararg keys: String): Ini? {
        if (keys.isEmpty())
            return null
        else {
            var current = core
            keys.forEach {
                current = current.findByName(it) ?: return null
            }
            return Ini(current)
        }
    }

    fun set(vararg keys: String, value: RawConfig) {
        if (keys.isEmpty())
            return
        var current = core
        keys.forEach {
            val sub = current.findByName(it)
            if (sub == null) {
                val new = RawConfig(it, arrayOf())
                current.subItems = (current.subItems?.plus(new)) ?: arrayOf(new)
                current = new
            } else {
                current = sub
            }
        }
        current.subItems = current.subItems?.map {
            if (it.name == value.name)
                value
            else
                it
        }?.toTypedArray() ?: arrayOf(value)
    }

    companion object {
        private external fun readFromIni(src: String): RawConfig?
        private external fun writeAsIni(dest: String, value: RawConfig)
        fun parseIniFromFile(file: File) =
            readFromIni(file.path)?.let { Ini(it) }

        fun writeIniToFile(ini: Ini, file: File) = writeAsIni(file.path, ini.core)
    }

}