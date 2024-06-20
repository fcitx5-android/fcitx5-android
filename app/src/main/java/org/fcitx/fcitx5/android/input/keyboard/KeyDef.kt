/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager

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
        val prefs: Prefs,
        val viewId: Int,
        val soundEffect: InputFeedbacks.SoundEffect
    ) {
        class Prefs(
            val keyBorder: ManagedPreference.PBool = ThemeManager.prefs.keyBorder,
            val keyRippleEffect: ManagedPreference.PBool = ThemeManager.prefs.keyRippleEffect,
            val keyRadius: ManagedPreference.PInt = ThemeManager.prefs.keyRadius,
            val keyHorizontalMargin: ManagedPreference.PInt = ThemeManager.prefs.keyHorizontalMargin,
            val keyVerticalMargin: ManagedPreference.PInt = ThemeManager.prefs.keyVerticalMargin,
            val keyHorizontalMarginLandscape: ManagedPreference.PInt = ThemeManager.prefs.keyHorizontalMarginLandscape,
            val keyVerticalMarginLandscape: ManagedPreference.PInt = ThemeManager.prefs.keyVerticalMarginLandscape,
        )

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
            prefs: Prefs = Prefs(),
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard,
        ) : Appearance(percentWidth, variant, border, margin, prefs, viewId, soundEffect)

        class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            textStyle: Int = Typeface.NORMAL,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            prefs: Prefs = Prefs(),
            viewId: Int = -1
        ) : Text(
            displayText,
            textSize,
            textStyle,
            percentWidth,
            variant,
            border,
            margin,
            prefs,
            viewId,
            InputFeedbacks.SoundEffect.Standard
        )

        class Image(
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            prefs: Prefs = Prefs(),
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard
        ) : Appearance(percentWidth, variant, border, margin, prefs, viewId, soundEffect)

        class ImageText(
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
            prefs: Prefs = Prefs(),
            viewId: Int = -1
        ) : Text(
            displayText,
            textSize,
            textStyle,
            percentWidth,
            variant,
            border,
            margin,
            prefs,
            viewId
        )
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