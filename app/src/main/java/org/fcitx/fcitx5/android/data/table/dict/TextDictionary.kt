/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.table.dict

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.TableManager
import org.fcitx.fcitx5.android.utils.errorArg
import java.io.File

class TextDictionary(file: File) : Dictionary() {

    override var file: File = file
        private set

    override val type: Type = Type.Text

    init {
        ensureFileExists()
        if (file.extension != type.ext)
            errorArg(R.string.exception_text_dict_filename, file.name)
    }

    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        file.copyTo(dest)
        return TextDictionary(dest)
    }

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary {
        ensureBin(dest)
        TableManager.tableDictConv(
            file.absolutePath,
            dest.absolutePath,
            TableManager.MODE_TXT_TO_BIN
        )
        return LibIMEDictionary(dest)
    }
}