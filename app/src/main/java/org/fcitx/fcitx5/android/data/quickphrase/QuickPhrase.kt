package org.fcitx.fcitx5.android.data.quickphrase

import java.io.File
import java.io.Serializable

abstract class QuickPhrase : Serializable {

    abstract val file: File

    abstract val isEnabled: Boolean

    open val name: String
        get() = file.nameWithoutExtension

    protected fun ensureFileExists() {
        if (!file.exists())
            throw IllegalStateException("File ${file.absolutePath} does not exist")
    }

    abstract fun loadData(): Result<QuickPhraseData>

    abstract fun saveData(data: QuickPhraseData)

    abstract fun enable()

    abstract fun disable()

    companion object {
        const val EXT = "mb"
        const val DISABLE = "disable"
    }
}