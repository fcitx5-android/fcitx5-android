package org.fcitx.fcitx5.android.data.pinyin.customphrase

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

    fun serialize() = "$key,${order.absoluteValue}=$value"
}
