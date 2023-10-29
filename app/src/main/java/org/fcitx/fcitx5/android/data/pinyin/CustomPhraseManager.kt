/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.pinyin

import org.fcitx.fcitx5.android.data.pinyin.customphrase.PinyinCustomPhrase

object CustomPhraseManager {
    @JvmStatic
    external fun load(): Array<PinyinCustomPhrase>?

    @JvmStatic
    external fun save(items: Array<PinyinCustomPhrase>)
}
