package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.CallSuper
import androidx.cardview.widget.CardView
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.resources.styledDrawable
import splitties.views.dsl.core.*
import splitties.views.gravityCenterVertical
import splitties.views.imageResource

abstract class BaseKeyView(context: Context) : CardView(context) {

    init {
        isClickable = true
        foreground = styledDrawable(android.R.attr.selectableItemBackground)
        isHapticFeedbackEnabled = false
    }

    val layout = verticalLayout {
        gravity = gravityCenterVertical
    }.also { add(it, lParams(matchParent, matchParent)) }

    @CallSuper
    open fun applyKey(key: BaseKey) {
        if (key is IKeyId)
            id = key.id
    }
}

class ImageKeyView(context: Context) : BaseKeyView(context) {

    val button = imageButton().also { layout.add(it, lParams(matchParent, wrapContent)) }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is IImageKey) {
            button.imageResource = key.src
            if (key is ITintKey) {
                button.backgroundTintList = styledColorSL(key.background)
                button.colorFilter =
                    PorterDuffColorFilter(
                        styledColor(key.foreground),
                        PorterDuff.Mode.SRC_IN
                    )
            }
        }
    }
}

open class TextKeyView(context: Context) : BaseKeyView(context) {

    val button = button {
        background = null
    }.also { layout.add(it, lParams(matchParent, wrapContent)) }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is ITextKey) {
            button.text = key.displayText
            button.textSize = 16f // sp
            button.isAllCaps = false
        }

    }
}

open class AltTextKeyView(context: Context) : TextKeyView(context) {
    val altText = textView().also { layout.add(it, lParams(wrapContent, wrapContent) {}) }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is AltTextKey) {
            button.text = key.text
            altText.text = key.altText
            altText.textSize = 14f // sp
            altText.isAllCaps = false
        }
    }
}
