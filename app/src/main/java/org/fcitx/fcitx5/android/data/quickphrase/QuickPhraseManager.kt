package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.data.DataManager
import org.fcitx.fcitx5.android.utils.appContext
import java.io.File
import java.io.InputStream

object QuickPhraseManager {

    private val builtinQuickPhraseDir = File(
        DataManager.dataDir, "usr/share/fcitx5/data/quickphrase.d"
    )

    private val customQuickPhraseDir = File(
        appContext.getExternalFilesDir(null)!!, "data/quickphrase.d"
    ).also { it.mkdirs() }

    fun listQuickPhrase(): List<QuickPhrase> =
        listDir(builtinQuickPhraseDir) { file ->
            BuiltinQuickPhrase(file)
        } + listDir(customQuickPhraseDir) { file ->
            CustomQuickPhrase(file)
        }

    fun newEmpty(name: String): CustomQuickPhrase {
        val file = File(customQuickPhraseDir, "$name.${QuickPhrase.EXT}")
        file.createNewFile()
        return CustomQuickPhrase(file)
    }

    fun importFromFile(file: File): CustomQuickPhrase {
        if (file.extension != QuickPhrase.EXT)
            throw IllegalArgumentException("${file.path} is not a quick phrase")
        // throw away data, only ensuring the format is correct
        QuickPhraseData.fromLines(file.readLines()).getOrThrow()
        val dest = File(customQuickPhraseDir, file.name)
        file.copyTo(dest)
        return CustomQuickPhrase(dest)
    }

    fun importFromInputStream(stream: InputStream, name: String): CustomQuickPhrase {
        val tempFile = File(appContext.cacheDir, name)
        tempFile.outputStream().use {
            stream.copyTo(it)
        }
        val new = importFromFile(tempFile)
        tempFile.delete()
        return new
    }


    private fun listDir(
        dir: File,
        block: (File) -> QuickPhrase
    ): List<QuickPhrase> =
        dir.listFiles()
            ?.mapNotNull { file ->
                file.extension.takeIf { ext -> ext == QuickPhrase.EXT }
                    ?.let { block(file) }
            } ?: listOf()


}