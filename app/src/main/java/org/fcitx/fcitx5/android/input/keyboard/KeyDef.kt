package org.fcitx.fcitx5.android.input.keyboard

import androidx.annotation.DrawableRes

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>,
    val popup: Array<Popup>? = null
) {
    sealed class Appearance(
        val percentWidth: Float,
        val variant: Variant,
        val forceBordered: Boolean,
        val viewId: Int,
    ) {
        enum class Variant {
            Normal, AltForeground, Alternative, Accent
        }

        open class Text(
            val displayText: String,
            val textSize: Float,
            val typeface: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            forceBordered: Boolean = false,
            viewId: Int = -1
        ) : Appearance(percentWidth, variant, forceBordered, viewId)

        class AltText(
            displayText: String,
            val altText: String,
            textSize: Float,
            typeface: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            forceBordered: Boolean = false,
            viewId: Int = -1
        ) : Text(displayText, textSize, typeface, percentWidth, variant, forceBordered, viewId)

        class Image(
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            forceBordered: Boolean = false,
            viewId: Int = -1
        ) : Appearance(percentWidth, variant, forceBordered, viewId)
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

    sealed class Popup {
        open class Preview(val content: String) : Popup()

        class AltPreview(content: String, val alternative: String) : Preview(content)

        class Keyboard(val keys: Array<Key>) : Popup() {
            data class Key(val str: String, val action: KeyAction)
        }
    }
}