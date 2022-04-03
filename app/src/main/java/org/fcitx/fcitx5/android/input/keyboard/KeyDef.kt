package org.fcitx.fcitx5.android.input.keyboard

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes

open class KeyDef(
    val appearance: Appearance,
    val behavior: Behavior
) {
    sealed class Appearance(
        val percentWidth: Float,
        @AttrRes
        val background: Int,
        val viewId: Int
    ) {
        open class Text(
            val displayText: String,
            val textSize: Float,
            val typeface: Int,
            percentWidth: Float = 0.1f,
            @AttrRes
            val textColor: Int = android.R.attr.colorForeground,
            background: Int = android.R.attr.colorButtonNormal,
            viewId: Int = -1
        ) : Appearance(percentWidth, background, viewId)

        class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            typeface: Int,
            percentWidth: Float = 0.1f,
            textColor: Int = android.R.attr.colorForeground,
            background: Int = android.R.attr.colorButtonNormal,
            viewId: Int = -1
        ) : Text(displayText, textSize, typeface, percentWidth, textColor, background, viewId)

        class Image(
            @DrawableRes
            val src: Int,
            @AttrRes
            val tint: Int,
            percentWidth: Float = 0.1f,
            background: Int = android.R.attr.colorButtonNormal,
            viewId: Int = -1
        ) : Appearance(percentWidth, background, viewId)
    }

    sealed class Behavior {
        open class Press(
            val action: KeyAction
        ) : Behavior()

        class Repeat(
            action: KeyAction
        ) : Press(action)

        class LongPress(
            action: KeyAction,
            val longPressAction: KeyAction
        ) : Press(action)
    }
}