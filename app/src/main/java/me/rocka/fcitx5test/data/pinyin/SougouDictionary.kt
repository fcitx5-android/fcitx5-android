package me.rocka.fcitx5test.data.pinyin

import me.rocka.fcitx5test.data.PinyinDictManager
import java.io.File

class SougouDictionary(file: File) : Dictionary() {
    override var file: File = file
        private set

    init {
        ensureFileExists()
        if (file.extension != EXT)
            throw IllegalArgumentException("Not a sougou dict ${file.name}")
    }

    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        PinyinDictManager.sougouDictConv(file.absolutePath, dest.absolutePath)
        return TextDictionary(dest)
    }

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary {
        val txtDict = toTextDictionary()
        val libimeDict = txtDict.toLibIMEDictionary(dest)
        txtDict.file.delete()
        return libimeDict
    }

    companion object {
        const val EXT = "scel"
    }
}