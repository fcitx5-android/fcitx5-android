/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.popup

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import android.text.TextPaint
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum
import org.fcitx.fcitx5.android.utils.includes

object EmojiModifier {

    enum class SkinTone(val value: String, override val stringRes: Int) : ManagedPreferenceEnum {
        Default("", R.string.emoji_skin_tone_none),
        Type_1_2("🏻", R.string.emoji_skin_tone_type_1_2),
        Type_3("🏼", R.string.emoji_skin_tone_type_3),
        Type_4("🏽", R.string.emoji_skin_tone_type_4),
        Type_5("🏾", R.string.emoji_skin_tone_type_5),
        Type_6("🏿", R.string.emoji_skin_tone_type_6)
    }

    /**
     * **Special Case 1:** Drop `U+FE0F` (Variation Selector-16) when combining with skin tone
     */
    private val SpecialCase1 = intArrayOf(
        0x261D,  // ☝️
        0x26F9,  // ⛹️
        0x270C,  // ✌️
        0x1F3CB, // 🏋️
        0x1F3CC, // 🏌️
        0x1F574, // 🕴️
        0x1F575, // 🕵️
        0x1F590, // 🖐️
    )
    private const val VariationSelector16 = 0xFE0F

    /**
     * **Special Case 2:** Make `U+1F91D`(🤝 Handshake) in 🧑‍🤝‍🧑 not modifiable
     */
    private val SpecialCase2 = intArrayOf(
        0x1F9D1, 0x200D, 0x1F91D, 0x200D, 0x1F9D1,
    )

    private val defaultSkinTone by AppPrefs.getInstance().symbols.defaultEmojiSkinTone

    fun isSupported(): Boolean {
        // UProperty.EMOJI_MODIFIER_BASE requires API 28
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun isModifiable(modifiable: BooleanArray): Boolean {
        val sum = modifiable.sumOf { if (it) 1 else 0 }
        // bail if too crowded
        // eg. https://emojipedia.org/family-man-medium-light-skin-tone-woman-medium-light-skin-tone-girl-medium-light-skin-tone-boy-medium-light-skin-tone
        return sum == 1 || sum == 2
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getCodePoints(emoji: String): Pair<IntArray, BooleanArray> {
        val codePoints = emoji.codePoints().toArray()
        val modifiable = BooleanArray(codePoints.size) {
            UCharacter.hasBinaryProperty(codePoints[it], UProperty.EMOJI_MODIFIER_BASE)
        }
        // make U+1F91D not modifiable if the whole sequence is special
        if (codePoints contentEquals SpecialCase2) {
            modifiable[2] = false
        }
        return codePoints to modifiable
    }

    private fun buildEmoji(codePoints: IntArray, modifiable: BooleanArray, tone: SkinTone): String {
        return buildString {
            for (i in 0..<codePoints.size) {
                // skip U+FE0F if the preceding character is special
                if (i > 0 && codePoints[i] == VariationSelector16 &&
                    SpecialCase1.includes(codePoints[i - 1]) &&
                    tone != SkinTone.Default
                ) continue
                appendCodePoint(codePoints[i])
                if (modifiable[i]) {
                    append(tone.value)
                }
            }
        }
    }

    private val DefaultTextPaint = TextPaint()

    fun getPreferredTone(emoji: String): String {
        if (!isSupported()) return emoji
        val (codePoints, modifiable) = getCodePoints(emoji)
        if (!isModifiable(modifiable)) return emoji
        val candidate = buildEmoji(codePoints, modifiable, defaultSkinTone)
        return if (DefaultTextPaint.hasGlyph(candidate)) candidate else emoji
    }

    fun produceSkinTones(emoji: String): Array<String>? {
        if (!isSupported()) return null
        val (codePoints, modifiable) = getCodePoints(emoji)
        if (!isModifiable(modifiable)) return null
        val candidates = SkinTone.entries
            .filter { it != defaultSkinTone }
            .map { buildEmoji(codePoints, modifiable, it) }
            .filter { DefaultTextPaint.hasGlyph(it) }
        return if (candidates.isEmpty()) null else candidates.toTypedArray()
    }
}
