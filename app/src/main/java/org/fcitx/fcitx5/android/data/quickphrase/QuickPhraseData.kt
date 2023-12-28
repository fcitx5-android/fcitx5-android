/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

class QuickPhraseData(private val data: List<QuickPhraseEntry>) : List<QuickPhraseEntry> by data {

    fun serialize(): String = joinToString("\n") { it.serialize() }

    companion object {
        // https://github.com/fcitx/fcitx5/blob/5.1.5/src/lib/fcitx-utils/macros.h#L46
        private val WhiteSpaces = charArrayOf(' ', '\t', '\r', '\n', '\u000b', '\u000c')

        // https://github.com/fcitx/fcitx5/blob/5.1.5/src/modules/quickphrase/quickphraseprovider.cpp#L67
        fun parseLine(line: String): QuickPhraseEntry? {
            if (line.isEmpty()) return null
            val s = line.trim()
            val pos = s.indexOfAny(WhiteSpaces)
            if (pos < 0) return null
            val word = s.substring(pos).indexOfFirst { c -> !WhiteSpaces.contains(c) }
            if (word < 0) return null
            return QuickPhraseEntry(s.substring(0, pos), s.substring(pos + word))
        }

        fun fromLines(lines: List<String>): QuickPhraseData {
            return QuickPhraseData(lines.mapNotNull { parseLine(it) })
        }
    }
}