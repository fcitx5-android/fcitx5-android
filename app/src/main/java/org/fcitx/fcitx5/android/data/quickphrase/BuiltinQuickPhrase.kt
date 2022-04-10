package org.fcitx.fcitx5.android.data.quickphrase

import java.io.File

class BuiltinQuickPhrase(
    override val file: File
) : QuickPhrase() {
    init {
        ensureFileExists()
        if (file.extension != EXT)
            throw IllegalArgumentException("Not a quickphrase file ${file.name}")
    }
}
