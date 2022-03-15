package org.fcitx.fcitx5.android.data.pinyin.dict

import org.fcitx.fcitx5.android.data.pinyin.PinyinDictManager
import java.io.File

class LibIMEDictionary(file: File) : Dictionary() {

    override var file: File = file
        private set

    var isEnabled: Boolean = true
        private set

    override val type: Type = Type.LibIME

    override val name: String
        get() = if (isEnabled) super.name
        else file.name.substringBefore(".${type.ext}.$DISABLE")

    init {
        ensureFileExists()
        isEnabled = when {
            file.extension == type.ext -> {
                true
            }
            file.name.endsWith(".${type.ext}.$DISABLE") -> {
                false
            }
            else -> throw IllegalArgumentException("Not a libime dict ${file.name}")
        }
    }

    fun enable() {
        if (isEnabled)
            return
        val newFile = file.resolveSibling(name + ".${type.ext}")
        file.renameTo(newFile)
        file = newFile
        isEnabled = true
    }

    fun disable() {
        if (!isEnabled)
            return
        val newFile = file.resolveSibling(name + ".${type.ext}.$DISABLE")
        file.renameTo(newFile)
        file = newFile
        isEnabled = false
    }

    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        PinyinDictManager.pinyinDictConv(
            file.absolutePath,
            dest.absolutePath,
            PinyinDictManager.MODE_BIN_TO_TXT
        )
        return TextDictionary(dest)
    }

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary {
        ensureBin(dest)
        file.copyTo(dest)
        return LibIMEDictionary(dest)
    }

    companion object {
        const val DISABLE = "disable"
    }
}