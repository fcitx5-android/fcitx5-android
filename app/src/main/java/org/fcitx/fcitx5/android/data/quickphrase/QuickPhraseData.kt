package org.fcitx.fcitx5.android.data.quickphrase

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
                                throw RuntimeException("Failed to parse quickphrase line $it")
                            QuickPhraseEntry(key, value)
                        }
                    }
            }.map { QuickPhraseData(it) }
    }
}