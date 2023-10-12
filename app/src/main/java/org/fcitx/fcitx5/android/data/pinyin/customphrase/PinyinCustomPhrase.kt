package org.fcitx.fcitx5.android.data.pinyin.customphrase

import kotlin.math.absoluteValue

data class PinyinCustomPhrase(
    var key: String,
    var order: Int,
    var value: String
) {
    var enabled: Boolean
        get() = order > 0
        set(newValue) {
            order = (if (newValue) 1 else -1) * order.absoluteValue
        }

    fun serialize() = "$key,${order.absoluteValue}=$value"
}
