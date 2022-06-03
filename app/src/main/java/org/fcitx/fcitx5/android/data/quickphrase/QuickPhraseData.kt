package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.errorRuntime

class QuickPhraseData(private val data: List<QuickPhraseEntry>) :
    List<QuickPhraseEntry> by data {

    fun serialize(): String = joinToString("\n") { it.serialize() }

    companion object {
        fun fromLines(lines: List<String>): Result<QuickPhraseData> =
            runCatching {
                lines
                    .mapNotNull {
                        it.trim().takeIf(String::isNotEmpty)?.let { l ->
                            val key = l.substringBefore(' ')
                            val value = l.substringAfter(' ')
                            if (key.isEmpty() || value.isEmpty())
                                errorRuntime(R.string.exception_quickphrase_parse, it)
                            QuickPhraseEntry(key, value)
                        }
                    }
            }.map { QuickPhraseData(it) }
    }
}