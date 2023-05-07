package org.fcitx.fcitx5.android.data.table

import cc.ekblad.konbini.ParserResult
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.utils.IniParser
import org.fcitx.fcitx5.android.utils.IniPrettyPrinter
import org.fcitx.fcitx5.android.utils.Locales
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.getValue
import timber.log.Timber
import java.io.File

class TableBasedInputMethod(val file: File) {

    private var ini = when (val x = IniParser.parse(file.readText())) {
        is ParserResult.Error -> errorRuntime(R.string.invalid_im, file.name)
        is ParserResult.Ok -> x.result
    }

    var table: LibIMEDictionary? = null

    val name: String by lazy {
        ini.sections[InputMethod]?.let { im ->
            val properties = im.data
            properties.getValue(NameI18n.format(Locales.languageWithCountry))
                ?: properties.getValue(NameI18n.format(Locales.language))
                ?: properties.getValue(Name)
        } ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_INPUT_METHOD_OR_NAME)
    }

    var tableFileName: String
        get() = ini.getValue(Table, File)
            ?.substringAfterLast('/')
            ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_TABLE_OR_FILE)
        set(value) {
            ini.setValue(Table, File, "table/$value")
        }

    val tableFileExists
        get() = table != null

    fun save() {
        file.writeText(IniPrettyPrinter.pretty(ini))
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