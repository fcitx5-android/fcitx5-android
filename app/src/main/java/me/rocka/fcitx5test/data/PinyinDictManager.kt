package me.rocka.fcitx5test.data

import me.rocka.fcitx5test.utils.appContext
import java.io.File

object PinyinDictManager {
    init {
        System.loadLibrary("pinyindictionaryutils")
    }

    private val pinyinDicDir = File(
        appContext.getExternalFilesDir(null)!!, "data/pinyin/dictionaries"
    )

    @JvmStatic
    private external fun pinyinDictConv(src: String, dest: String, mode: Boolean)

    private const val MODE_BIN_TO_TXT = true
    private const val MODE_TXT_TO_BIN = false

}