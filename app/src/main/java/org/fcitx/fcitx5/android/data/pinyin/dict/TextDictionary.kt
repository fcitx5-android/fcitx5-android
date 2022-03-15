package org.fcitx.fcitx5.android.data.pinyin.dict

import org.fcitx.fcitx5.android.data.pinyin.PinyinDictManager
import java.io.File

class TextDictionary(file: File) : Dictionary() {
    override var file: File = file
        private set

    override val type: Type = Type.Text

    init {
        ensureFileExists()
        if (file.extension != type.ext)
            throw IllegalArgumentException("Not a text dict ${file.name}")
    }

    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        file.copyTo(dest)
        return TextDictionary(dest)
    }

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary {
        ensureBin(dest)
        PinyinDictManager.pinyinDictConv(
            file.absolutePath,
            dest.absolutePath,
            PinyinDictManager.MODE_TXT_TO_BIN
        )
        return LibIMEDictionary(dest)
    }
}