package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.cardview.widget.CardView
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter
import splitties.views.imageResource

abstract class BaseKeyView(context: Context) : FrameLayout(context) {

    init {
        isClickable = true
        isHapticFeedbackEnabled = false
    }

    val layout = verticalLayout(matchParent) {
        gravity = gravityVerticalCenter
    }

    val card = view(::CardView) {
        radius = dp(4f)
        cardElevation = dp(2f)
        add(layout, lParams(matchParent, matchParent))
    }

    @CallSuper
    open fun applyKey(key: BaseKey) {
        if (key is IKeyId) {
            id = key.id
        }
        add(card, lParams(matchParent, matchParent) {
            horizontalMargin = dp(2)
            verticalMargin = dp(5)
        })
    }
}

class ImageKeyView(context: Context) : BaseKeyView(context) {

    val img = imageView {
        isClickable = false
        isFocusable = false
    }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is IImageKey) {
            img.imageResource = key.src
            if (key is ITintKey) {
                card.setCardBackgroundColor(styledColorSL(key.background))
                img.colorFilter =
                    PorterDuffColorFilter(styledColor(key.foreground), PorterDuff.Mode.SRC_IN)
            }
        }
        layout.add(img, lParams(matchParent, wrapContent))
    }
}

open class TextKeyView(context: Context) : BaseKeyView(context) {

    val mainText = textView {
        isClickable = false
        isFocusable = false
        background = null
        textSize = 20f // sp
        gravity = gravityCenter
        setTextColor(styledColor(android.R.attr.colorForeground))
    }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is ITextKey) {
            mainText.text = key.displayText
        }
        layout.add(mainText, lParams(matchParent))
    }
}

open class AltTextKeyView(context: Context) : TextKeyView(context) {

    val altText = textView {
        isClickable = false
        isFocusable = false
        textSize = 12f // sp
        gravity = gravityCenter
    }

    override fun applyKey(key: BaseKey) {
        super.applyKey(key)
        if (key is AltTextKey) {
            mainText.text = key.text
            altText.text = key.altText
        }
        layout.add(altText, lParams(matchParent))
    }
}
