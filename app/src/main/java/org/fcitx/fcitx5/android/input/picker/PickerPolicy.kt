/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.popup.EmojiModifier
import org.fcitx.fcitx5.android.input.popup.PopupPreset

interface PickerPolicy {
    /**
     * Whether Picker should include this symbol
     */
    fun filter(raw: String): Boolean

    /**
     * Actual string to commit when this symbol was selected
     */
    fun transform(raw: String): String

    /** Recently used items keep their original text unless this policy opts in. */
    fun transformRecent(raw: String): String = raw

    /**
     * Long press options for this symbol
     */
    fun popup(raw: String): KeyDef.Popup.Keyboard?

    /**
     * Return different value to make the Picker rebuild all pages on attach
     */
    fun invalidateKey(): Any
}

/**
 * Default policy: show all symbols as provided and never need to rebuild
 */
class DefaultPickerPolicy : PickerPolicy {
    override fun filter(raw: String) = true
    override fun transform(raw: String) = raw
    override fun popup(raw: String) =
        KeyDef.Popup.Keyboard.Preset(label = raw, transformPunctuation = false)

    override fun invalidateKey() = Unit
}

/** Symbol-page punctuation defaults, independent of the active input engine. */
class SymbolPickerPolicy : PickerPolicy {
    private val enabled by AppPrefs.getInstance().symbols.fullWidthSymbolPunctuation

    override fun filter(raw: String) = true

    private fun transformPunctuation(raw: String): String =
        if (enabled) FullWidthPunctuation[raw] ?: raw else raw

    override fun transform(raw: String): String =
        transformPunctuation(raw)

    override fun transformRecent(raw: String) = transform(raw)

    override fun popup(raw: String): KeyDef.Popup.Keyboard? {
        if (!enabled) {
            return KeyDef.Popup.Keyboard.Preset(label = raw, transformPunctuation = false)
        }
        val halfWidth = FullWidthPunctuation.entries.firstOrNull { it.value == raw }?.key ?: raw
        val variants = PopupPreset[halfWidth]?.map(::transformPunctuation).orEmpty()
        val items = if (halfWidth in FullWidthPunctuation) listOf(halfWidth) + variants else variants
        return items.takeIf { it.isNotEmpty() }?.let {
            KeyDef.Popup.Keyboard.Explicit(it.distinct().toTypedArray())
        }
    }

    override fun invalidateKey() = enabled

    companion object {
        private val FullWidthPunctuation = mapOf(
            "," to "，", "." to "。", "?" to "？", "!" to "！", ":" to "：",
            ";" to "；", "(" to "（", ")" to "）", "'" to "“", "\"" to "”"
        )
    }
}

/**
 * Emoji policy: filter unsupported glyph, transform skintone, rebuild on preference changes
 */
class EmojiPickerPolicy : PickerPolicy {
    data class Prefs(
        val hideUnsupportedEmojis: Boolean,
        val defaultSkinTone: EmojiModifier.SkinTone
    )

    private val symbolPrefs = AppPrefs.getInstance().symbols
    private val hideUnsupportedEmojis by symbolPrefs.hideUnsupportedEmojis
    private val defaultSkinTone by symbolPrefs.defaultEmojiSkinTone

    private val prefs get() = Prefs(hideUnsupportedEmojis, defaultSkinTone)

    override fun filter(raw: String): Boolean {
        return if (hideUnsupportedEmojis) EmojiModifier.isValidEmoji(raw) else true
    }

    override fun transform(raw: String): String {
        return EmojiModifier.getPreferredTone(raw, defaultSkinTone)
    }

    override fun popup(raw: String): KeyDef.Popup.Keyboard? {
        val items = EmojiModifier.produceSkinTones(raw, defaultSkinTone) ?: return null
        return KeyDef.Popup.Keyboard.Explicit(items)
    }

    override fun invalidateKey(): Prefs = prefs
}
