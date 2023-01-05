package org.fcitx.fcitx5.android.data.table.dict

import org.fcitx.fcitx5.android.data.table.TableManager
import java.io.File

class LibIMEDictionary(file: File) : Dictionary() {

    override var file: File = file
        private set

    override val type: Type = Type.LibIME

    override val name: String
        get() = file.name.substringBefore(".${type.ext}")

    init {
        ensureFileExists()
    }

    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        TableManager.tableDictConv(
            file.absolutePath,
            dest.absolutePath,
            TableManager.MODE_BIN_TO_TXT
        )
        return TextDictionary(dest)
    }

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary {
        ensureBin(dest)
        file.copyTo(dest)
        return LibIMEDictionary(dest)
    }
}