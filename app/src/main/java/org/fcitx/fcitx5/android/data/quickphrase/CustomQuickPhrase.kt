package org.fcitx.fcitx5.android.data.quickphrase

import java.io.File

class CustomQuickPhrase(file: File) : QuickPhrase() {

    var isEnabled = false
        private set

    override var file: File = file
        private set

    override val name: String
        get() = if (isEnabled) super.name
        else file.name.substringBefore(".$EXT.$DISABLE")

    init {
        ensureFileExists()
        isEnabled = when {
            file.extension == EXT -> {
                true
            }
            file.name.endsWith(".$EXT.$DISABLE") -> {
                false
            }
            else -> throw IllegalArgumentException("Not a quickphrase file ${file.name}")
        }
    }

    fun enable() {
        if (isEnabled)
            return
        val newFile = file.resolveSibling("$name.$EXT")
        file.renameTo(newFile)
        file = newFile
        isEnabled = true
    }

    fun disable() {
        if (!isEnabled)
            return
        val newFile = file.resolveSibling("$name.$EXT.$DISABLE")
        file.renameTo(newFile)
        file = newFile
        isEnabled = false
    }

    fun saveData(data: QuickPhraseData) =
        file.writeText(data.serialize())

    companion object {
        const val DISABLE = "disable"
    }

}
