package org.fcitx.fcitx5.android.input.keyboard

import androidx.annotation.DrawableRes

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>
) {
    sealed class Appearance(
        val percentWidth: Float,
        val viewId: Int,
    ) {
        open class Text(
            val displayText: String,
            val textSize: Float,
            val typeface: Int,
            val forceBordered: Boolean = false,
            val isFunKey: Boolean = false,
            percentWidth: Float = 0.1f,
            viewId: Int = -1
        ) : Appearance(percentWidth, viewId)

        class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            typeface: Int,
            forceBordered: Boolean = false,
            isFunKey: Boolean = false,
            percentWidth: Float = 0.1f,
            viewId: Int = -1
        ) : Text(displayText, textSize, typeface, forceBordered, isFunKey, percentWidth, viewId)

        class Image(
            @DrawableRes
            val src: Int,
            val accentBackground: Boolean = false,
            percentWidth: Float = 0.1f,
            viewId: Int = -1
        ) : Appearance(percentWidth, viewId)
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