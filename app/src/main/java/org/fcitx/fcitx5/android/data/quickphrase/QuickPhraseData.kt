/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.errorRuntime

class QuickPhraseData(private val data: List<QuickPhraseEntry>) : List<QuickPhraseEntry> by data {

    fun serialize(): String = joinToString("\n") { it.serialize() }

    companion object {
        fun fromLines(lines: List<String>): Result<QuickPhraseData> {
            return runCatching {
                val list = mutableListOf<QuickPhraseEntry>()
                lines.forEach {
                    if (it.isBlank()) return@forEach
                    val s = it.trim()
                    val sep = s.indexOf(' ')
                    if (sep < 0)
                        errorRuntime(R.string.exception_quickphrase_parse, it)
                    list.add(QuickPhraseEntry(s.substring(0, sep), s.substring(sep + 1)))
                }
                QuickPhraseData(list)
            }
        }
    }
}