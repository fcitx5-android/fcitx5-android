/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

class QuickPhraseData(private val data: List<QuickPhraseEntry>) : List<QuickPhraseEntry> by data {

    fun serialize(): String = joinToString("\n") { it.serialize() }

    companion object {
        fun fromLines(lines: List<String>): QuickPhraseData {
            return QuickPhraseData(lines.mapNotNull { QuickPhraseEntry.fromLine(it) })
        }
    }
}