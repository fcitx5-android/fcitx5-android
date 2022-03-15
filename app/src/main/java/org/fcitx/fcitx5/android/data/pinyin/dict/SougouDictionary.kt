package org.fcitx.fcitx5.android.data.pinyin.dict

import org.fcitx.fcitx5.android.data.pinyin.PinyinDictManager
import java.io.File

class SougouDictionary(file: File) : Dictionary() {
    override var file: File = file
        private set

    override val type: Type = Type.Sougou

    init {
        ensureFileExists()
        if (file.extension != type.ext)
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

}