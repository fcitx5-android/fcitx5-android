/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.pinyin.dict

import java.io.File

abstract class PinyinDictionary {

    enum class Type(val ext: String) {
        LibIME("dict"), Sougou("scel"), Text("txt");

        companion object {
            fun fromFileName(name: String): Type? =
                when {
                    name.endsWith(".dict.disable") -> LibIME
                    name.endsWith(".dict") -> LibIME
                    name.endsWith(".scel") -> Sougou
                    name.endsWith(".txt") -> Text
                    else -> null
                }
        }
    }

    abstract val file: File

    abstract val type: Type

    abstract fun toTextDictionary(dest: File): TextDictionary

    abstract fun toLibIMEDictionary(dest: File): LibIMEDictionary

    open val name: String
        get() = file.nameWithoutExtension

    fun toTextDictionary(): TextDictionary {
        val dest = file.resolveSibling(name + ".${Type.Text.ext}")
        return toTextDictionary(dest)
    }

    fun toLibIMEDictionary(): LibIMEDictionary {
        val dest = file.resolveSibling(name + ".${Type.LibIME.ext}")
        return toLibIMEDictionary(dest)
    }

    protected fun ensureFileExists() {
        if (!file.exists())
            throw IllegalStateException("File ${file.absolutePath} does not exist")
    }

    protected fun ensureTxt(dest: File) {
        if (dest.extension != Type.Text.ext)
            throw IllegalArgumentException("Dest file name must end with .${Type.Text.ext}")
        dest.delete()
    }

    protected fun ensureBin(dest: File) {
        if (dest.extension != Type.LibIME.ext)
            throw IllegalArgumentException("Dest file name must end with .${Type.LibIME.ext}")
        dest.delete()
    }

    override fun toString(): String = "${javaClass.simpleName}[$name -> ${file.path}]"

    companion object {
        fun new(it: File): PinyinDictionary? = when (Type.fromFileName(it.name)) {
            Type.LibIME -> LibIMEDictionary(it)
            Type.Sougou -> SougouDictionary(it)
            Type.Text -> TextDictionary(it)
            null -> null
        }
    }
}