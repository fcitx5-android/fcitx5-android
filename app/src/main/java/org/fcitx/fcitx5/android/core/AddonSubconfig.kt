package org.fcitx.fcitx5.android.core

suspend fun Fcitx.reloadPinyinDict() = setAddonSubConfig("pinyin", "dictmanager")

suspend fun Fcitx.getPunctuationConfig(lang: String) =
    getAddonSubConfig("punctuation", "punctuationmap/$lang")

suspend fun Fcitx.savePunctuationConfig(lang: String = "zh_CN", config: RawConfig) =
    setAddonSubConfig("punctuation", "punctuationmap/$lang", config)