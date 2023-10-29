/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.table

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.table.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.utils.Ini
import org.fcitx.fcitx5.android.utils.Locales
import org.fcitx.fcitx5.android.utils.errorRuntime
import timber.log.Timber
import java.io.File

class TableBasedInputMethod(val file: File) {

    private var ini = Ini.parseIniFromFile(file) ?: errorRuntime(R.string.invalid_im, file.name)

    var table: LibIMEDictionary? = null

    val name: String by lazy {
        ini.get(InputMethod)?.let {
            (it.get(NameI18n.format(Locales.languageWithCountry))
                ?: it.get(NameI18n.format(Locales.language))
                ?: it.get(Name))?.value
        } ?: errorRuntime(
            R.string.invalid_im,
            ERROR_MISSING_INPUT_METHOD_OR_NAME
        )
    }

    var tableFileName: String
        get() = ini.get(Table, File)?.value
            ?.substringAfterLast('/')
            ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_TABLE_OR_FILE)
        set(value) {
            ini.set(Table, File, str = "table/$value")
        }

    val tableFileExists
        get() = table != null

    fun save() {
        Ini.writeIniToFile(ini, file)
    }

    fun delete() {
        table?.file?.delete()
        table = null
        file.delete()
    }

    companion object {
        const val InputMethod = "InputMethod"
        const val Name = "Name"
        const val NameI18n = "Name[%s]"
        const val Table = "Table"
        const val File = "File"

        const val ERROR_MISSING_INPUT_METHOD_OR_NAME = "missing [InputMethod] section or 'Name' key"
        const val ERROR_MISSING_TABLE_OR_FILE = "missing [Table] section or 'File' key"

        fun fixedTableFileName(name: String) =
            name.split(' ')
                .joinToString(separator = "-")
                .lowercase() + ".main.dict"

        fun new(configFile: File): TableBasedInputMethod {
            val im = TableBasedInputMethod(configFile)
            Timber.d("new TableBasedInputMethod(name=${im.name}, tableFileName=${im.tableFileName})")
            return im
        }
    }
}