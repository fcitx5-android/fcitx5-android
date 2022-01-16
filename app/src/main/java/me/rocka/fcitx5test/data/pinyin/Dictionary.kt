package me.rocka.fcitx5test.data.pinyin

import java.io.File

abstract class Dictionary {

    abstract val file: File

    abstract fun toTextDictionary(dest: File): TextDictionary

    abstract fun toLibIMEDictionary(dest: File): LibIMEDictionary

    fun toTextDictionary(): TextDictionary {
        val dest = file.resolveSibling(file.nameWithoutExtension + ".${TextDictionary.EXT}")
        return toTextDictionary(dest)
    }

    fun toLibIMEDictionary(): LibIMEDictionary {
        val dest = file.resolveSibling(file.nameWithoutExtension + ".${LibIMEDictionary.EXT}")
        return toLibIMEDictionary(dest)
    }

    protected fun ensureFileExists() {
        if (!file.exists())
            throw IllegalStateException("File ${file.absolutePath} does not exist")
    }

    protected fun ensureTxt(dest: File) {
        if (dest.extension != TextDictionary.EXT)
            throw IllegalArgumentException("Dest file name must end with .txt")
        dest.deleteOnExit()
    }

    protected fun ensureBin(dest: File) {
        if (dest.extension != LibIMEDictionary.EXT)
            throw IllegalArgumentException("Dest file name must end with .dict")
        dest.deleteOnExit()
    }

    override fun toString(): String = "${javaClass.simpleName}[${file.path}]"

    companion object {
        fun new(it: File): Dictionary? = when {
            it.extension == LibIMEDictionary.EXT ||
                    it.name.endsWith(".${LibIMEDictionary.EXT}.${LibIMEDictionary.DISABLE}")
            -> LibIMEDictionary(it)
            it.extension == SougouDictionary.EXT -> SougouDictionary(it)
            it.extension == TextDictionary.EXT -> TextDictionary(it)
            else -> null
        }
    }
}