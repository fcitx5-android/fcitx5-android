/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

object DoublePinyin {

    object Xiaohe {

        private val hints = mapOf(
            "Q" to "iu",
            "W" to "ia",
            "E" to "ua",
            "R" to "uan",
            "T" to "ue",
            "Y" to "ing",
            "U" to "sh",
            "I" to "ch",
            "O" to "uo",
            "P" to "ie",

            "A" to "a",
            "S" to "ong",
            "D" to "ai",
            "F" to "en",
            "G" to "eng",
            "H" to "ang",
            "J" to "an",
            "K" to "uai",
            "L" to "uang",

            "Z" to "ei",
            "X" to "ie",
            "C" to "ao",
            "V" to "ui",
            "B" to "ou",
            "N" to "in",
            "M" to "ian"
        )

        fun getHint(character: String): String? {
            return hints[character.uppercase()]
        }
    }
}