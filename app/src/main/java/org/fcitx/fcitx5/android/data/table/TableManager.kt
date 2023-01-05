package org.fcitx.fcitx5.android.data.table

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.dict.Dictionary
import org.fcitx.fcitx5.android.data.table.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.extract
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object TableManager {

    init {
        System.loadLibrary("tabledictionaryutils")
    }

    private val inputMethodDir = File(
        appContext.getExternalFilesDir(null)!!, "data/inputmethod"
    ).also { it.mkdirs() }

    private val tableDicDir = File(
        appContext.getExternalFilesDir(null)!!, "data/table"
    ).also { it.mkdirs() }

    fun inputMethods() =
        inputMethodDir
            .listFiles()
            ?.mapNotNull { im ->
                TableBasedInputMethod.new(im)?.apply {
                    table = runCatching {
                        File(
                            tableDicDir,
                            tableFileName
                        ).takeIf { it.extension == "dict" }?.let {
                            LibIMEDictionary(it)
                        }
                    }.getOrNull()
                }
            }
            ?: listOf()

    fun importTableBasedIM(src: InputStream): Result<TableBasedInputMethod> =
        runCatching {
            ZipInputStream(src).use { zipStream ->
                val extracted = zipStream.extract()
                val im =
                    (extracted.find { it.name.endsWith(".conf") }
                        ?: extracted.find { it.name.endsWith(".conf.in") }
                        ?: errorRuntime(R.string.exception_table_im)).let {
                        val f = File(inputMethodDir, it.name.removeSuffix(".in"))
                        it.copyTo(f)
                        TableBasedInputMethod.new(f) ?: run {
                            f.delete()
                            errorRuntime(R.string.invalid_im)
                        }
                    }
                val table =
                    (extracted.find { it.name.endsWith(".dict") || it.name.endsWith(".txt") }
                        ?: errorRuntime(R.string.exception_table)).let {
                        Dictionary.new(it)!!
                    }
                im.tableFileName = TableBasedInputMethod.fixedTableFileName(table.name)
                im.table = table.toLibIMEDictionary(File(tableDicDir, im.tableFileName))
                im.save()
                im
            }
        }

    @JvmStatic
    external fun tableDictConv(src: String, dest: String, mode: Boolean)

    const val MODE_BIN_TO_TXT = true
    const val MODE_TXT_TO_BIN = false
}