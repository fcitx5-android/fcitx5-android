package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import org.fcitx.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.imageDrawable

abstract class KeyView(ctx: Context, val def: KeyDef.Appearance) : FrameLayout(ctx) {
    val layout = verticalLayout(matchParent) {
        gravity = gravityCenter
        // sync any state from parent
        isDuplicateParentStateEnabled = true
    }

    val card = view(::CardView) {
        radius = dp(4f)
        setCardBackgroundColor(styledColorSL(def.background))
        // sync pressed state from parent
        isDuplicateParentStateEnabled = true
        // pressed highlight
        foreground = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorControlHighlight))
            })
        }
        add(layout, lParams(matchParent, matchParent))
    }

    init {
        // trigger setEnabled(true)
        isEnabled = true
        isClickable = true
        isHapticFeedbackEnabled = false
        if (def.viewId > 0) {
            id = def.viewId
        }
        add(card, lParams(matchParent, matchParent) {
            horizontalMargin = dp(2)
            verticalMargin = dp(5)
        })
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        card.cardElevation = if (enabled) dp(2f) else 0f
        layout.alpha = if (enabled) 1f else styledFloat(android.R.attr.disabledAlpha)
    }
}

@SuppressLint("ViewConstructor")
open class TextKeyView(ctx: Context, def: KeyDef.Appearance.Text) :
    KeyView(ctx, def) {
    val mainText = textView {
        isClickable = false
        isFocusable = false
        background = null
        setTextColor(styledColor(android.R.attr.colorForeground))
        text = def.displayText
        textSize = def.textSize
        typeface = Typeface.defaultFromStyle(def.typeface)
    }

    init {
        layout.add(mainText, lParams())
    }
}

@SuppressLint("ViewConstructor")
class AltTextKeyView(ctx: Context, def: KeyDef.Appearance.AltText) : TextKeyView(ctx, def) {
    val altText = textView {
        isClickable = false
        isFocusable = false
        // hardcoded text size for now
        textSize = 12f
        text = def.altText
    }

    init {
        layout.add(altText, lParams())
    }
}

@SuppressLint("ViewConstructor")
class ImageKeyView(ctx: Context, def: KeyDef.Appearance.Image) : KeyView(ctx, def) {
    val img = imageView {
        isClickable = false
        isFocusable = false
        imageDrawable = drawable(def.src)
        colorFilter = PorterDuffColorFilter(styledColor(def.tint), PorterDuff.Mode.SRC_IN)
    }

    init {
        layout.add(img, lParams())
    }
}
