package org.fcitx.fcitx5.android.data.punctuation

import org.fcitx.fcitx5.android.core.*

object PunctuationManager {

    fun parseRawConfig(raw: RawConfig): List<PunctuationMapEntry> {
        return raw.findByName("cfg")
            ?.run { get(ENTRIES).subItems?.map { PunctuationMapEntry(it) } }
            ?: listOf()
    }

    suspend fun load(fcitx: FcitxAPI, lang: String): List<PunctuationMapEntry> {
        val raw = fcitx.getPunctuationConfig(lang)
        return parseRawConfig(raw)
    }

    suspend fun save(fcitx: FcitxAPI, lang: String, entries: List<PunctuationMapEntry>) {
        val cfg = RawConfig(
            arrayOf(
                RawConfig(ENTRIES, entries.mapIndexed { i, it -> it.toRawConfig(i) }.toTypedArray())
            )
        )
        fcitx.savePunctuationConfig(lang, cfg)
    }

    const val ENTRIES = "Entries"
    const val KEY = "Key"
    const val MAPPING = "Mapping"
    const val ALT_MAPPING = "AltMapping"
}