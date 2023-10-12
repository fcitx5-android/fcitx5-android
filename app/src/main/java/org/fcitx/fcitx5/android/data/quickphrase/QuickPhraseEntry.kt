package org.fcitx.fcitx5.android.data.quickphrase

data class QuickPhraseEntry(val keyword: String, val phrase: String) {
    fun serialize() = "$keyword ${phrase.replace("\n", "\\n")}"
}