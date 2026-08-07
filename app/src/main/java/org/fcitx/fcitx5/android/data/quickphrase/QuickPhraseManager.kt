/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.withTempDir
import java.io.File
import java.io.InputStream

object QuickPhraseManager {

    private val builtinQuickPhraseDir = File(
        DataManager.dataDir, "usr/share/fcitx5/data/quickphrase.d"
    )

    private val customQuickPhraseDir = File(
        appContext.getExternalFilesDir(null) ?: appContext.filesDir,
        "data/data/quickphrase.d"
    ).also { it.mkdirs() }

    val commonWords: CustomQuickPhrase by lazy {
        val file = File(customQuickPhraseDir, "$COMMON_WORDS_FILE_NAME.${QuickPhrase.EXT}")
        if (!file.exists()) file.createNewFile()
        CustomQuickPhrase(file)
    }

    fun isCommonWords(quickPhrase: QuickPhrase): Boolean =
        quickPhrase.file.absolutePath == commonWords.file.absolutePath

    fun interface OnCommonWordsChangedListener {
        fun onChanged(entries: List<QuickPhraseEntry>)
    }

    private val commonWordsListeners = WeakHashSet<OnCommonWordsChangedListener>()

    fun addOnCommonWordsChangedListener(listener: OnCommonWordsChangedListener) {
        commonWordsListeners.add(listener)
    }

    fun removeOnCommonWordsChangedListener(listener: OnCommonWordsChangedListener) {
        commonWordsListeners.remove(listener)
    }

    @Synchronized
    fun loadCommonWords(): List<QuickPhraseEntry> = commonWords.loadData().toList()

    @Synchronized
    fun saveCommonWords(entries: List<QuickPhraseEntry>) {
        commonWords.saveData(QuickPhraseData(entries))
        val snapshot = entries.toList()
        commonWordsListeners.forEach { it.onChanged(snapshot) }
    }

    @Synchronized
    fun deleteCommonWord(entry: QuickPhraseEntry): List<QuickPhraseEntry> {
        val entries = loadCommonWords().toMutableList()
        entries.remove(entry)
        saveCommonWords(entries)
        return entries
    }

    fun listQuickPhrase(): List<QuickPhrase> {
        val builtin = listDir(builtinQuickPhraseDir) { file ->
            BuiltinQuickPhrase(file, File(customQuickPhraseDir, file.name))
        }
        val custom = listDir(customQuickPhraseDir) { file ->
            CustomQuickPhrase(file).takeUnless { cq -> builtin.any { cq.name == it.name } }
        }
        return builtin + custom
    }

    fun newEmpty(name: String): CustomQuickPhrase {
        val file = File(customQuickPhraseDir, "$name.${QuickPhrase.EXT}")
        file.createNewFile()
        return CustomQuickPhrase(file)
    }

    private fun importFromFile(file: File): Result<CustomQuickPhrase> {
        return runCatching {
            // check quickphrase format of each line
            file.readLines().forEachIndexed { idx, line ->
                if (line.isNotBlank() && QuickPhraseEntry.fromLine(line) == null) {
                    errorRuntime(R.string.exception_quickphrase_parse, "\n(${idx + 1}) $line")
                }
            }
            val dest = File(customQuickPhraseDir, file.name)
            file.copyTo(dest)
            CustomQuickPhrase(dest)
        }
    }

    fun importFromInputStream(stream: InputStream, fileName: String): Result<CustomQuickPhrase> {
        return stream.use { i ->
            withTempDir { dir ->
                val tempFile = dir.resolve(fileName)
                tempFile.outputStream().use { o -> i.copyTo(o) }
                importFromFile(tempFile)
            }
        }
    }

    private fun <T : QuickPhrase> listDir(
        dir: File,
        block: (File) -> T?
    ): List<T> =
        dir.listFiles()
            ?.mapNotNull { file ->
                file.name.takeIf { name ->
                    name.endsWith(".${QuickPhrase.EXT}") || name.endsWith(".${QuickPhrase.EXT}.${QuickPhrase.DISABLE}")
                }
                    ?.let { block(file) }
            } ?: listOf()

    private const val COMMON_WORDS_FILE_NAME = "f5clipboard-common-words"
}
