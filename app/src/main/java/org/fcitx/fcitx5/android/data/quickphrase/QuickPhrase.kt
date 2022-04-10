package org.fcitx.fcitx5.android.data.quickphrase

import java.io.File
import java.io.Serializable

abstract class QuickPhrase : Serializable {

    abstract val file: File

    open val name: String
        get() = file.nameWithoutExtension

    protected fun ensureFileExists() {
        if (!file.exists())
            throw IllegalStateException("File ${file.absolutePath} does not exist")
    }

    fun loadData() = QuickPhraseData.fromLines(file.readLines())

    companion object {
        const val EXT = "mb"
    }
}