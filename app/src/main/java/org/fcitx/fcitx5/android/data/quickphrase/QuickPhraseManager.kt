/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.withTempDir
import java.io.File
import java.io.InputStream

object QuickPhraseManager {

    private val builtinQuickPhraseDir = File(
        DataManager.dataDir, "usr/share/fcitx5/data/quickphrase.d"
    )

    private val customQuickPhraseDir = File(
        appContext.getExternalFilesDir(null)!!, "data/data/quickphrase.d"
    ).also { it.mkdirs() }

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
        // throw away data, only ensuring the format is correct
        return QuickPhraseData.fromLines(file.readLines()).map {
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


}