package me.rocka.fcitx5test.data.pinyin

import me.rocka.fcitx5test.data.PinyinDictManager
import java.io.File

class LibIMEDictionary(file: File) : Dictionary() {

    override var file: File = file
        private set

    var isEnabled: Boolean = true
        private set

    init {
        ensureFileExists()
        isEnabled = when {
            file.extension == EXT -> {
                true
            }
            file.name.endsWith(".$EXT.$DISABLE") -> {
                false
            }
            else -> throw IllegalArgumentException("Not a libime dict ${file.name}")
        }
    }

    fun enable() {
        if (isEnabled)
            return
        val newFile = file.resolveSibling(file.nameWithoutExtension + ".$EXT")
        file.renameTo(newFile)
        file = newFile
        isEnabled = true
    }

    fun disable() {
        if (!isEnabled)
            return
        val newFile = file.resolveSibling(file.nameWithoutExtension + ".$EXT.$DISABLE")
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
        const val EXT = "dict"
        const val DISABLE = "disable"
    }
}