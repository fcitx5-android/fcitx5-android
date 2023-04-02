package org.fcitx.fcitx5.android.data.table

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.utils.FcitxIni
import org.fcitx.fcitx5.android.utils.Locales
import org.fcitx.fcitx5.android.utils.errorRuntime
import timber.log.Timber
import java.io.File

class TableBasedInputMethod(private val ini: FcitxIni, val file: File) {

    var table: LibIMEDictionary? = null

    val name: String by lazy {
        Name.format()
        ini[InputMethod]?.let { im ->
            im[NameI18n.format(Locales.languageWithCountry)]
                ?: im[NameI18n.format(Locales.language)]
                ?: im[Name]
        } ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_INPUT_METHOD_OR_NAME)
    }

    var tableFileName: String
        get() =
            ini.get(Table, File)?.let { File(it).name }
                ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_TABLE_OR_FILE)
        set(value) {
            ini.put(Table, File, "table/$value")
                ?: errorRuntime(R.string.invalid_im, ERROR_MISSING_TABLE_OR_FILE)
        }

    val tableFileExists
        get() = table != null

    fun save() {
        ini.store()
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
            val im = TableBasedInputMethod(FcitxIni(configFile), configFile)
            Timber.d("new TableBasedInputMethod(name=${im.name}, tableFileName=${im.tableFileName})")
            return im
        }
    }
}