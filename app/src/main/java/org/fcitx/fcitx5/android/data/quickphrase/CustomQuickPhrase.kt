/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.errorArg
import java.io.File

class CustomQuickPhrase(file: File) : QuickPhrase() {

    override var isEnabled = false
        private set

    override var file: File = file
        private set

    override val name: String
        get() = if (isEnabled) super.name
        else file.name.substringBefore(".$EXT.$DISABLE")

    override fun loadData(): Result<QuickPhraseData> = QuickPhraseData.fromLines(file.readLines())

    init {
        ensureFileExists()
        isEnabled = when {
            file.extension == EXT -> {
                true
            }
            file.name.endsWith(".$EXT.$DISABLE") -> {
                false
            }
            else -> errorArg(R.string.exception_quickphrase_filename, file.name)
        }
    }

    override fun enable() {
        if (isEnabled)
            return
        val newFile = file.resolveSibling("$name.$EXT")
        file.renameTo(newFile)
        file = newFile
        isEnabled = true
    }

    override fun disable() {
        if (!isEnabled)
            return
        val newFile = file.resolveSibling("$name.$EXT.$DISABLE")
        file.renameTo(newFile)
        file = newFile
        isEnabled = false
    }

    override fun saveData(data: QuickPhraseData) =
        file.writeText(data.serialize())

    override fun toString(): String {
        return "CustomQuickPhrase(isEnabled=$isEnabled, file=$file, name='$name')"
    }

}
