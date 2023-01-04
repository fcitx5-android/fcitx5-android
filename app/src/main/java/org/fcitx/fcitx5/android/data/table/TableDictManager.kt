package org.fcitx.fcitx5.android.data.table

object TableDictManager {

    init {
        System.loadLibrary("tabledictionaryutils")
    }

    @JvmStatic
    external fun tableDictConv(src: String, dest: String, mode: Boolean)

    const val MODE_BIN_TO_TXT = true
    const val MODE_TXT_TO_BIN = false
}