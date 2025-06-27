/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.popup

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import android.text.TextPaint

val EmojiSkinTones = arrayOf("🏻", "🏼", "🏽", "🏾", "🏿")

val DefaultTextPaint = TextPaint()

fun getEmojiWithSkinTones(emoji: String): Array<String>? {
    // UProperty.EMOJI_MODIFIER_BASE requires API 28
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return null
    }
    val codePoints = emoji.codePoints().toArray()
    val isEmojiModifierBase = Array(codePoints.size) {
        UCharacter.hasBinaryProperty(codePoints[it], UProperty.EMOJI_MODIFIER_BASE)
    }
    val sum = isEmojiModifierBase.sumOf { if (it) 1 else 0 }
    if (sum == 0 || sum > 2) {
        // bail if too crowded; eg. https://emojipedia.org/family-man-medium-light-skin-tone-woman-medium-light-skin-tone-girl-medium-light-skin-tone-boy-medium-light-skin-tone
        return null
    }
    val candidates = EmojiSkinTones.map { tone ->
        buildString {
            codePoints.forEachIndexed { index, codePoint ->
                append(Character.toString(codePoint))
                if (isEmojiModifierBase[index]) append(tone)
            }
        }
    }.filter { DefaultTextPaint.hasGlyph(it) }
    return if (candidates.isEmpty()) null else candidates.toTypedArray()
}
