package org.fcitx.fcitx5.android.core

suspend fun FcitxAPI.reloadPinyinDict() = setAddonSubConfig("pinyin", "dictmanager")

suspend fun FcitxAPI.getPunctuationConfig(lang: String) =
    getAddonSubConfig("punctuation", "punctuationmap/$lang")

suspend fun FcitxAPI.savePunctuationConfig(lang: String = "zh_CN", config: RawConfig) =
    setAddonSubConfig("punctuation", "punctuationmap/$lang", config)

suspend fun FcitxAPI.reloadQuickPhrase() = setAddonSubConfig("quickphrase", "editor")