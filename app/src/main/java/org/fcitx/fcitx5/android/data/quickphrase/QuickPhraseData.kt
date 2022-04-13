package org.fcitx.fcitx5.android.data.quickphrase

class QuickPhraseData(private val data: Map<String, String>) :
    Map<String, String> by data {

    fun serialize(): String =
        toList().joinToString("\n") { (a, b) -> "$a $b" }

    companion object {
        fun fromLines(lines: List<String>): Result<QuickPhraseData> =
            runCatching {
                lines
                    .mapNotNull {
                        it.trim().takeIf(String::isNotEmpty)
                    }.associate {
                        val key = it.substringBefore(' ')
                        val value = it.substringAfter(' ')
                        if (key.isEmpty() || value.isEmpty())
                            throw RuntimeException("Failed to parse quickphrase line $it")
                        key to value
                    }
            }.map { QuickPhraseData(it) }
    }
}