/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class ShuangpinLayout(override val stringRes: Int) : ManagedPreferenceEnum {
    None(R.string.none),
    Xiaohe(R.string.shuangpin_xiaohe),
    Ziranma(R.string.shuangpin_ziranma),
    Microsoft(R.string.shuangpin_microsoft),
    Sogou(R.string.shuangpin_sogou),
    PinyinJiaJia(R.string.shuangpin_pinyin_jiajia);

    fun getMapping(char: Char): String? {
        val upperChar = char.uppercaseChar()
        return when (this) {
            None -> null
            Xiaohe -> xiaoheMapping[upperChar]
            Ziranma -> ziranmaMapping[upperChar]
            Microsoft -> microsoftMapping[upperChar]
            Sogou -> sogouMapping[upperChar]
            PinyinJiaJia -> pinyinJiaJiaMapping[upperChar]
        }
    }

    companion object {
        private val xiaoheMapping = mapOf(
            'Q' to "iu", 'W' to "ei", 'E' to "e", 'R' to "uan", 'T' to "ue", 'Y' to "un",
            'U' to "u/sh", 'I' to "i/ch", 'O' to "uo/o", 'P' to "ie", 'A' to "a", 'S' to "ong/iong",
            'D' to "ai", 'F' to "en", 'G' to "eng", 'H' to "ang", 'J' to "an", 'K' to "ing/uai",
            'L' to "iang/uang", 'Z' to "ou", 'X' to "ia/ua", 'C' to "ao", 'V' to "ui/zh",
            'B' to "in", 'N' to "iao", 'M' to "ian"
        )

        private val ziranmaMapping = mapOf(
            'Q' to "iu", 'W' to "ia/ua", 'E' to "e", 'R' to "uan", 'T' to "ue/ve", 'Y' to "ing/uai",
            'U' to "u/sh", 'I' to "i/ch", 'O' to "uo/o", 'P' to "un", 'A' to "a", 'S' to "ong/iong",
            'D' to "iang/uang", 'F' to "en", 'G' to "eng", 'H' to "ang", 'J' to "an", 'K' to "ao",
            'L' to "ai", 'Z' to "ei", 'X' to "ie", 'C' to "iao", 'V' to "ui/v/zh", 'B' to "ou",
            'N' to "in", 'M' to "ian"
        )

        private val microsoftMapping = mapOf(
            'Q' to "iu", 'W' to "ia/ua", 'E' to "e", 'R' to "uan/er", 'T' to "ue", 'Y' to "uai",
            'U' to "u/sh", 'I' to "i/ch", 'O' to "uo/o", 'P' to "un", 'A' to "a", 'S' to "ong/iong",
            'D' to "iang/uang", 'F' to "en", 'G' to "eng", 'H' to "ang", 'J' to "an", 'K' to "ao",
            'L' to "ai", 'Z' to "ei", 'X' to "ie", 'C' to "iao", 'V' to "ui/v/zh", 'B' to "ou",
            'N' to "in", 'M' to "ian"
        )

        private val sogouMapping = mapOf(
            'Q' to "iu", 'W' to "ia/ua", 'E' to "e", 'R' to "uan/er", 'T' to "ue/ve", 'Y' to "uai",
            'U' to "u/sh", 'I' to "i/ch", 'O' to "uo/o", 'P' to "un", 'A' to "a", 'S' to "ong/iong",
            'D' to "iang/uang", 'F' to "en", 'G' to "eng", 'H' to "ang", 'J' to "an", 'K' to "ao",
            'L' to "ai", 'Z' to "ei", 'X' to "ie", 'C' to "iao", 'V' to "ui/v/zh", 'B' to "ou",
            'N' to "in", 'M' to "ian"
        )

        private val pinyinJiaJiaMapping = mapOf(
            'Q' to "ing", 'W' to "ei", 'E' to "e", 'R' to "en", 'T' to "eng", 'Y' to "ong/iong",
            'U' to "u/ch", 'I' to "i/sh", 'O' to "uo/o", 'P' to "ou", 'A' to "a", 'S' to "ai",
            'D' to "ao", 'F' to "an", 'G' to "ang", 'H' to "iang/uang", 'J' to "ian", 'K' to "iao",
            'L' to "in", 'Z' to "un", 'X' to "ue/uai", 'C' to "uan", 'V' to "ui/v/zh", 'B' to "ia/ua",
            'N' to "iu", 'M' to "ie"
        )
    }
}
