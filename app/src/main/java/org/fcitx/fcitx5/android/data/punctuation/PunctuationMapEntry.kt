/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.punctuation

import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager.ALT_MAPPING
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager.KEY
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager.MAPPING

data class PunctuationMapEntry(val key: String, val mapping: String, val altMapping: String) {
    constructor(it: RawConfig) : this(
        it[KEY].value,
        it[MAPPING].value,
        it[ALT_MAPPING].value
    )

    fun toRawConfig(idx: Int) = RawConfig(
        idx.toString(), arrayOf(
            RawConfig(KEY, key),
            RawConfig(MAPPING, mapping),
            RawConfig(ALT_MAPPING, altMapping),
        )
    )
}