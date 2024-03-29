/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.core.FcitxUtils

data class QuickPhraseEntry(val keyword: String, val phrase: String) {

    fun serialize() = "$keyword ${FcitxUtils.escapeForValue(phrase)}"

    companion object {
        // https://github.com/fcitx/fcitx5/blob/5.1.5/src/lib/fcitx-utils/macros.h#L46
        private val WhiteSpaces = charArrayOf(' ', '\t', '\r', '\n', '\u000b', '\u000c')

        // https://github.com/fcitx/fcitx5/blob/5.1.5/src/modules/quickphrase/quickphraseprovider.cpp#L67
        fun fromLine(line: String): QuickPhraseEntry? {
            val text = line.trim()
            if (text.isEmpty()) return null
            val pos = text.indexOfAny(WhiteSpaces)
            if (pos < 0) return null
            val word = text.substring(pos).indexOfFirst { c -> !WhiteSpaces.contains(c) }
            if (word < 0) return null
            return try {
                val wordString = FcitxUtils.unescapeForValue(text.substring(pos + word))
                val key = text.substring(0, pos)
                QuickPhraseEntry(key, wordString)
            } catch (e: Exception) {
                null
            }
        }
    }
}