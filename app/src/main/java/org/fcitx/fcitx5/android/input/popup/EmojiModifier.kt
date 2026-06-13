/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.popup

import android.annotation.SuppressLint
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.UnicodeSet
import android.os.Build
import android.text.TextPaint
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

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
    @RequiresApi(Build.VERSION_CODES.P)
    private fun shouldSkipVariationSelector16(ch: Int): Boolean {
        return UCharacter.hasBinaryProperty(ch, UProperty.EMOJI_MODIFIER_BASE)
                && !UCharacter.hasBinaryProperty(ch, UProperty.EMOJI_PRESENTATION)
    }

    private const val VariationSelector16 = 0xFE0F

    /**
     * **Special Case 2:** Make `U+1F91D`(🤝 Handshake) in 🧑‍🤝‍🧑 not modifiable
     */
    private val SpecialCase2 = intArrayOf(
        0x1F9D1, 0x200D, 0x1F91D, 0x200D, 0x1F9D1,
    )

    fun isSupported(): Boolean {
        // UProperty.EMOJI_MODIFIER_BASE requires API 28
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun isModifiable(modifiable: BooleanArray): Boolean {
        val sum = modifiable.count { it }
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildEmoji(codePoints: IntArray, modifiable: BooleanArray, tone: SkinTone): String {
        return buildString {
            var i = 0
            while (i < codePoints.size) {
                appendCodePoint(codePoints[i])
                if (modifiable[i]) {
                    append(tone.value)
                    if (tone != SkinTone.Default &&
                        codePoints.getOrNull(i + 1) == VariationSelector16 &&
                        shouldSkipVariationSelector16(codePoints[i])
                    ) i++
                }
                i++
            }
        }
    }

    private val DefaultTextPaint by lazy {
        TextPaint()
    }

    private val RGIEmojiSet by lazy {
        @SuppressLint("NewApi")
        UnicodeSet("[:RGI_Emoji:]").freeze()
    }

    private fun isValidEmoji(emoji: String): Boolean {
        // UProperty.RGI_EMOJI is available on 34+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!RGIEmojiSet.contains(emoji)) return false
        }
        return DefaultTextPaint.hasGlyph(emoji)
    }

    fun getPreferredTone(emoji: String, tone: SkinTone): String {
        if (!isSupported()) return emoji
        val (codePoints, modifiable) = getCodePoints(emoji)
        if (tone == SkinTone.Default || !isModifiable(modifiable)) return emoji
        val candidate = buildEmoji(codePoints, modifiable, tone)
        return if (isValidEmoji(candidate)) candidate else emoji
    }

    fun produceSkinTones(emoji: String, excludeTone: SkinTone): Array<String>? {
        if (!isSupported()) return null
        val (codePoints, modifiable) = getCodePoints(emoji)
        if (!isModifiable(modifiable)) return null
        val candidates = SkinTone.entries
            .filter { it != excludeTone }
            .map { buildEmoji(codePoints, modifiable, it) }
            .filter { isValidEmoji(it) }
        return if (candidates.isEmpty()) null else candidates.toTypedArray()
    }
}
