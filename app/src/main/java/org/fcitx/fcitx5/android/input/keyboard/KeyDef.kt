package org.fcitx.fcitx5.android.input.keyboard

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.utils.resource.ColorResource

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>
) {
    sealed class Appearance(
        val percentWidth: Float,
        val background: ColorResource? = null,
        val viewId: Int
    ) {
        open class Text(
            val displayText: String,
            val textSize: Float,
            val typeface: Int,
            percentWidth: Float = 0.1f,
            val textColor: ColorResource? = null,
            background: ColorResource? = null,
            viewId: Int = -1
        ) : Appearance(percentWidth, background, viewId)

        class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            typeface: Int,
            percentWidth: Float = 0.1f,
            textColor: ColorResource? = null,
            background: ColorResource? = null,
            viewId: Int = -1
        ) : Text(displayText, textSize, typeface, percentWidth, textColor, background, viewId)

        class Image(
            @DrawableRes
            val src: Int,
            val tint: ColorResource? = null,
            percentWidth: Float = 0.1f,
            background: ColorResource? = null,
            viewId: Int = -1
        ) : Appearance(percentWidth, background, viewId)
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

        class SwipeDown(
            val action: KeyAction
        ) : Behavior()

        class DoubleTap(
            val action: KeyAction
        ) : Behavior()
    }
}