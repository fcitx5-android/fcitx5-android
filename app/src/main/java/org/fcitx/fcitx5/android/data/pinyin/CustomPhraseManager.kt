package org.fcitx.fcitx5.android.data.pinyin

import org.fcitx.fcitx5.android.data.pinyin.customphrase.PinyinCustomPhrase

object CustomPhraseManager {
    @JvmStatic
    external fun load(): Array<PinyinCustomPhrase>?

    @JvmStatic
    external fun save(items: Array<PinyinCustomPhrase>)
}
