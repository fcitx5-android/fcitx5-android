/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry

object StatusIconMapping {
    @DrawableRes
    fun fromEntry(entry: InputMethodEntry): Int {
        if (entry.subMode.icon.isNotEmpty()) {
            when (entry.subMode.icon) {
                "fcitx_mozc_direct" -> return R.drawable.ic_status_latin_direct
                "fcitx_mozc_hiragana" -> return R.drawable.ic_status_hiragana
                "fcitx_mozc_katakana_full" -> return R.drawable.ic_status_katakana
                "fcitx_mozc_alpha_half" -> return R.drawable.ic_status_latin_half_underscore
                "fcitx_mozc_alpha_full" -> return R.drawable.ic_status_latin_wide
                "fcitx_mozc_katakana_half" -> return R.drawable.ic_status_katakana_half_underscore
            }
        }
        if (entry.subMode.label.isNotEmpty()) {
            when (entry.subMode.label) {
                "あ" -> return R.drawable.ic_status_hiragana
                "ア" -> return R.drawable.ic_status_katakana
                "ｱ" -> return R.drawable.ic_status_katakana_half
                "Ａ" -> return R.drawable.ic_status_latin_wide
                "A" -> return R.drawable.ic_status_latin_direct
            }
        }
        when (entry.icon) {
            "fcitx-pinyin" -> return R.drawable.ic_status_pinyin
            "fcitx-shuangpin" -> return R.drawable.ic_status_shuangpin
            "fcitx-wubi", "fcitx-wbpy" -> return R.drawable.ic_status_wubi
            "fcitx-cangjie" -> return R.drawable.ic_status_cangjie
            "fcitx-ziranma" -> return R.drawable.ic_status_ziranma
            "fcitx-dianbaoma" -> return R.drawable.ic_status_dianbaoma
            "fcitx-zhengma", "fcitx_zhengma" -> return R.drawable.ic_status_zhengma
            "fcitx-jyutping", "fcitx_jyutping_table" -> return R.drawable.ic_status_jyutping
        }
        when (entry.languageCode) {
            "en", "en_US" -> return R.drawable.ic_status_en
            "zh", "zh_CN", "zh_TW", "zh_HK" -> return R.drawable.ic_status_zh
            "ja" -> return R.drawable.ic_status_hiragana
            "ko" -> return R.drawable.ic_status_hangul
            "vi" -> return R.drawable.ic_status_vi
        }
        return R.drawable.ic_baseline_keyboard_24
    }
}
