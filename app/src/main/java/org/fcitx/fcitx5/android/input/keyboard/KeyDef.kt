/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.InputFeedbacks

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>,
    val popup: List<Popup>? = null  // ✅ 改为 List 替代 Array
) {
    sealed class Appearance(
        val percentWidth: Float,
        val variant: Variant,
        val border: Border,
        val margin: Boolean,
        val viewId: Int,
        val soundEffect: InputFeedbacks.SoundEffect
    ) {
        enum class Variant {
            Normal, AltForeground, Alternative, Accent
        }

        enum class Border {
            Default, On, Off, Special
        }

        data class Text(  // ✅ 改为 data class
            val displayText: String,
            val textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            val textStyle: Int = Typeface.NORMAL,
            val percentWidth: Float = 0.1f,
            val variant: Variant = Variant.Normal,
            val border: Border = Border.Default,
            val margin: Boolean = true,
            val viewId: Int = -1,
            val soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)

        data class AltText(  // ✅ 改为 data class
            val displayText: String,
            val altText: String,
            val textSize: Float,
            val doublePinyinHint: String? = null,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            val textStyle: Int = Typeface.NORMAL,
            val percentWidth: Float = 0.1f,
            val variant: Variant = Variant.Normal,
            val border: Border = Border.Default,
            val margin: Boolean = true,
            val viewId: Int = -1,
            val soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)

        data class Image(  // ✅ 改为 data class
            @DrawableRes
            val src: Int,
            val percentWidth: Float = 0.1f,
            val variant: Variant = Variant.Normal,
            val border: Border = Border.Default,
            val margin: Boolean = true,
            val viewId: Int = -1,
            val soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)

        data class ImageText(  // ✅ 改为 data class
            val displayText: String,
            val textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            val textStyle: Int = Typeface.NORMAL,
            @DrawableRes
            val src: Int,
            val percentWidth: Float = 0.1f,
            val variant: Variant = Variant.Normal,
            val border: Border = Border.Default,
            val margin: Boolean = true,
            val viewId: Int = -1,
            val soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)
    }

    sealed class Behavior {
        data class Press(  // ✅ 改为 data class
            val action: KeyAction
        ) : Behavior()

        data class LongPress(  // ✅ 改为 data class
            val action: KeyAction
        ) : Behavior()

        data class Repeat(  // ✅ 改为 data class
            val action: KeyAction
        ) : Behavior()

        data class Swipe(  // ✅ 改为 data class
            val action: KeyAction
        ) : Behavior()

        data class DoubleTap(  // ✅ 改为 data class
            val action: KeyAction
        ) : Behavior()
    }

    sealed class Popup {
        data class Preview(val content: String) : Popup()  // ✅ 改为 data class

        data class AltPreview(  // ✅ 改为 data class
            val content: String,
            val alternative: String
        ) : Popup()

        sealed class Keyboard : Popup() {
            data class Preset(
                val label: String,
                val transformPunctuation: Boolean = true
            ) : Keyboard()

            data class Explicit(
                val items: List<String>  // ✅ 改为 List 替代 Array
            ) : Keyboard()
        }

        data class Menu(  // ✅ 改为 data class
            val items: List<Item>  // ✅ 改为 List 替代 Array
        ) : Popup() {
            data class Item(  // ✅ 改为 data class
                val label: String,
                @DrawableRes val icon: Int,
                val action: KeyAction
            )
        }
    }
}
