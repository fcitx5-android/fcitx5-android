package org.fcitx.fcitx5.android.data.table

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.utils.FcitxIni
import org.fcitx.fcitx5.android.utils.Locales
import org.fcitx.fcitx5.android.utils.errorRuntime
import java.io.File

class TableBasedInputMethod(private val ini: FcitxIni, val file: File) {

    var table: LibIMEDictionary? = null

    val name by lazy {
        ini["InputMethod"]?.let { im ->
            im["Name[${Locales.languageWithCountry}]"]
                ?: im["Name[${Locales.language}]"]
                ?: im["Name"]
        } ?: errorRuntime(R.string.invalid_im)
    }

    var tableFileName
        get() = ini["Table"]?.let { table -> table["File"]?.let { File(it).name } }
            ?: errorRuntime(R.string.invalid_im)
        set(value) {
            ini["Table"]?.let { table ->
                table["File"] = "table/$value"
            } ?: errorRuntime(R.string.invalid_im)
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
        fun fixedTableFileName(name: String) =
            name.split(' ')
                .joinToString(separator = "-")
                .lowercase() + ".main.dict"

        fun new(configFile: File): TableBasedInputMethod? = runCatching {
            FcitxIni(configFile).takeIf { it.containsKey("Table") }
                ?.let { TableBasedInputMethod(it, configFile) }
        }.getOrNull()
    }
}