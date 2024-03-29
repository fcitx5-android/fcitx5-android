/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.pinyin.customphrase

import org.fcitx.fcitx5.android.core.FcitxUtils
import kotlin.math.absoluteValue

data class PinyinCustomPhrase(
    val key: String,
    val order: Int,
    val value: String
) {
    val enabled: Boolean get() = order > 0

    fun copyEnabled(e: Boolean): PinyinCustomPhrase {
        return copy(order = (if (e) 1 else -1) * order.absoluteValue)
    }

    fun serialize() = "$key,${order.absoluteValue}=${FcitxUtils.escapeForValue(value)}"
}
