/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.InputFeedbacks

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>,
    val popup: Array<Popup>? = null
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

        open class Text(
            val displayText: String,
            val textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            val textStyle: Int = Typeface.NORMAL,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)

        class TextLayoutSwitch(
            displayText: String,
            percentWidth: Float,
            variant: Variant
        ) : Text(
            displayText,
            textSize = 16f,
            textStyle = Typeface.BOLD,
            percentWidth = percentWidth,
            variant = variant
        )

        class NumPadNum(
            displayText: String,
            percentWidth: Float,
            variant: Variant
        ) : Text(
            displayText,
            textSize = 30f,
            percentWidth = percentWidth,
            variant = variant
        )

        class NumRow(digit: String) : Text(
            displayText = digit,
            textSize = 21f,
            border = Border.Off,
            margin = false
        )

        class Symbol(
            symbol: String,
            percentWidth: Float,
            variant: Variant
        ) : Text(
            displayText = symbol,
            textSize = 23f,
            percentWidth = percentWidth,
            variant = variant
        )

        class Alphabet(
            character: String,
            punctuation: String,
            variant: Variant
        ) : AltText(
            displayText = character,
            altText = punctuation,
            altTextSize = 10.666667f,
            textSize = 23f,
            variant = variant
        )

        object Space : Text(
            displayText = " ",
            textSize = 13f,
            percentWidth = 0f,
            border = Border.Special,
            viewId = R.id.button_space,
            soundEffect = InputFeedbacks.SoundEffect.SpaceBar
        )

        class Return(percentWidth: Float) : Image(
            src = R.drawable.ic_baseline_keyboard_return_24,
            percentWidth = percentWidth,
            variant = Variant.Accent,
            border = Border.Special,
            viewId = R.id.button_return,
            soundEffect = InputFeedbacks.SoundEffect.Return
        )

        class Comma(
            percentWidth: Float,
            variant: Variant,
        ) : ImageText(
            displayText = ",",
            textSize = 23f,
            percentWidth = percentWidth,
            variant = variant,
            src = R.drawable.ic_baseline_tag_faces_24
        )

        class Backspace(
            percentWidth: Float,
            variant: Variant,
        ) : Image(
            src = R.drawable.ic_baseline_backspace_24,
            percentWidth = percentWidth,
            variant = variant,
            border = Border.Special,
            viewId = R.id.button_backspace,
            soundEffect = InputFeedbacks.SoundEffect.Delete
        )

        object Caps : Image(
            src = R.drawable.ic_capslock_none,
            viewId = R.id.button_caps,
            percentWidth = 0.15f,
            variant = Variant.Alternative
        )

        open class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            val altTextSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            textStyle: Int = Typeface.NORMAL,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
        ) : Text(displayText, textSize, textStyle, percentWidth, variant, border, margin, viewId)

        open class Image(
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, viewId, soundEffect)

        open class ImageText(
            displayText: String,
            textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            textStyle: Int = Typeface.NORMAL,
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1
        ) : Text(displayText, textSize, textStyle, percentWidth, variant, border, margin, viewId)
    }

    sealed class Behavior {
        class Press(
            val action: KeyAction
        ) : Behavior()

        class LongPress(
            val action: KeyAction
        ) : Behavior()

        class Repeat(
            val action: KeyAction
        ) : Behavior()

        class Swipe(
            val action: KeyAction
        ) : Behavior()

        class DoubleTap(
            val action: KeyAction
        ) : Behavior()
    }

    sealed class Popup {
        open class Preview(val content: String) : Popup()

        class AltPreview(content: String, val alternative: String) : Preview(content)

        class Keyboard(val label: String) : Popup()

        class Menu(val items: Array<Item>) : Popup() {
            class Item(val label: String, @DrawableRes val icon: Int, val action: KeyAction)
        }
    }
}