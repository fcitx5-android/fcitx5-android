package org.fcitx.fcitx5.android.data.quickphrase

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.errorRuntime

class QuickPhraseData(private val data: List<QuickPhraseEntry>) :
    List<QuickPhraseEntry> by data {

    fun serialize(): String = joinToString("\n") { it.serialize() }

    companion object {
        fun fromLines(lines: List<String>): Result<QuickPhraseData> =
            runCatching {
                lines.filter { it.isNotBlank() }
                    .map {
                        val s = it.trim()
                        val spaceIndex = s.indexOf(' ')
                        if (spaceIndex < 0)
                            errorRuntime(R.string.exception_quickphrase_parse, it)
                        QuickPhraseEntry(s.substring(0, spaceIndex), s.substring(spaceIndex + 1))
                    }
            }.map { QuickPhraseData(it) }
    }
}