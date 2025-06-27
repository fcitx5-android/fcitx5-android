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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return null
    }
    val codePoints = emoji.codePoints().toArray()
    val isEmojiModifierBase = Array(codePoints.size) {
        UCharacter.hasBinaryProperty(codePoints[it], UProperty.EMOJI_MODIFIER_BASE)
    }
    if (isEmojiModifierBase.sumOf { if (it) 1 else 0 } > 2) {
        // bail if too crowded; eg. https://emojipedia.org/family-man-medium-light-skin-tone-woman-medium-light-skin-tone-girl-medium-light-skin-tone-boy-medium-light-skin-tone
        return null
    }
    if (UCharacter.hasBinaryProperty(codePoints[0], UProperty.EMOJI_MODIFIER_BASE)) {
        val candidates = Array(EmojiSkinTones.size) { i ->
            val tone = EmojiSkinTones[i]
            buildString {
                codePoints.forEachIndexed { j, codePoint ->
                    append(Character.toString(codePoint))
                    if (isEmojiModifierBase[j]) append(tone)
                }
            }
        }.filter { DefaultTextPaint.hasGlyph(it) }
        return if (candidates.isEmpty()) null else candidates.toTypedArray()
    }
    return null
}
