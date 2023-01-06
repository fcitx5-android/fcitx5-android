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

    fun inputMethods(): List<TableBasedInputMethod> =
        inputMethodDir
            .listFiles()
            ?.mapNotNull { confFile ->
                runCatching {
                    TableBasedInputMethod.new(confFile).apply {
                        table = runCatching {
                            File(tableDicDir, tableFileName)
                                .takeIf { it.extension == "dict" }
                                ?.let { LibIMEDictionary(it) }
                        }.getOrNull()
                    }
                }.getOrNull()
            } ?: listOf()

    fun importTableBasedIM(src: InputStream): Result<TableBasedInputMethod> =
        runCatching {
            ZipInputStream(src).use { zipStream ->
                val extracted = zipStream.extract()
                val confFile =
                    extracted.find { it.name.endsWith(".conf") || it.name.endsWith(".conf.in") }
                        ?: errorRuntime(R.string.exception_table_im)
                val im = confFile.let {
                    val importedConfFile = File(inputMethodDir, it.name.removeSuffix(".in"))
                    if (importedConfFile.exists())
                        errorRuntime(R.string.table_already_exists, it.name)
                    it.copyTo(importedConfFile)
                    runCatching {
                        TableBasedInputMethod.new(importedConfFile)
                    }.getOrElse { t: Throwable ->
                        importedConfFile.delete()
                        errorRuntime(R.string.invalid_im, t.message)
                    }
                }
                val dictFile =
                    extracted.find { it.name.endsWith(".dict") || it.name.endsWith(".txt") }
                        ?: errorRuntime(R.string.exception_table)
                val table = Dictionary.new(dictFile)!!
                im.tableFileName = TableBasedInputMethod.fixedTableFileName(table.name)
                runCatching {
                    im.table = table.toLibIMEDictionary(File(tableDicDir, im.tableFileName))
                }.onFailure { t: Throwable ->
                    im.file.delete()
                    errorRuntime(R.string.invalid_table_dict, t.message)
                }
                im.save()
                im
            }
        }

    @JvmStatic
    external fun tableDictConv(src: String, dest: String, mode: Boolean)

    @JvmStatic
    external fun checkTableDictFormat(src: String, user: Boolean = false): Boolean

    const val MODE_BIN_TO_TXT = true
    const val MODE_TXT_TO_BIN = false
}