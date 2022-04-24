package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import org.fcitx.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

abstract class KeyView(ctx: Context, val theme: Theme, val def: KeyDef.Appearance) :
    CustomGestureView(ctx) {
    val layout = constraintLayout {
        // sync any state from parent
        isDuplicateParentStateEnabled = true
    }

    val card = view(::CardView) {
        radius = dp(ThemeManager.prefs.keyRadius.getValue().toFloat())
        cardElevation = 0f
        if (ThemeManager.prefs.keyBorder.getValue() || def.forceBordered) {
            setCardBackgroundColor(
                when (def.variant) {
                    Variant.Normal -> theme.keyBackgroundColor
                    Variant.Alternative -> theme.altKeyBackgroundColor
                    Variant.Accent -> theme.accentKeyBackgroundColor
                }
            )
        } else {
            background = null
        }

        // sync pressed state from parent
        isDuplicateParentStateEnabled = true
        // pressed highlight
        foreground =
            if (ThemeManager.prefs.keyRippleEffect.getValue())
                rippleDrawable(theme.keyPressHighlightColor)
            else pressHighlightDrawable(theme.keyPressHighlightColor)
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
            horizontalMargin = dp(ThemeManager.prefs.keyHorizontalMargin.getValue())
            verticalMargin = dp(ThemeManager.prefs.keyVerticalMargin.getValue())
        })
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        layout.alpha = if (enabled) 1f else styledFloat(android.R.attr.disabledAlpha)
    }
}

@SuppressLint("ViewConstructor")
open class TextKeyView(ctx: Context, theme: Theme, def: KeyDef.Appearance.Text) :
    KeyView(ctx, theme, def) {
    val mainText = textView {
        isClickable = false
        isFocusable = false
        background = null
        text = def.displayText
        textSize = def.textSize
        typeface = Typeface.defaultFromStyle(def.typeface)
        setTextColor(
            when (def.variant) {
                Variant.Normal -> theme.keyTextColor
                Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }
        )
    }

    init {
        layout.apply {
            add(mainText, lParams(wrapContent, wrapContent) {
                centerInParent()
                verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            })
        }
    }
}

@SuppressLint("ViewConstructor")
class AltTextKeyView(ctx: Context, theme: Theme, def: KeyDef.Appearance.AltText) :
    TextKeyView(ctx, theme, def) {
    val altText = textView {
        isClickable = false
        isFocusable = false
        // hardcoded text size for now
        textSize = 12f
        text = def.altText
        // TODO darken altText color
        setTextColor(
            when (def.variant) {
                Variant.Normal -> theme.keyTextColor
                Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }
        )
    }

    init {
        layout.apply { add(altText, lParams(wrapContent, wrapContent)) }
        applyLayout(resources.configuration.orientation)
    }

    private fun applyLayout(orientation: Int) = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            mainText.updateLayoutParams<ConstraintLayout.LayoutParams> { endOfParent() }
            altText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topOfParent()
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                startToStart = ConstraintLayout.LayoutParams.UNSET
                endOfParent(dp(4))
            }
        }
        else -> {
            mainText.updateLayoutParams<ConstraintLayout.LayoutParams> { above(altText) }
            altText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                below(mainText)
                bottomOfParent()
                startOfParent()
                endOfParent()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        applyLayout(newConfig.orientation)
    }
}

@SuppressLint("ViewConstructor")
class ImageKeyView(ctx: Context, theme: Theme, def: KeyDef.Appearance.Image) :
    KeyView(ctx, theme, def) {
    val img = imageView {
        isClickable = false
        isFocusable = false
        imageDrawable = drawable(def.src)
        colorFilter = PorterDuffColorFilter(
            when (def.variant) {
                // always apply alternative text color to image key
                Variant.Normal, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            },
            PorterDuff.Mode.SRC_IN
        )
    }

    init {
        layout.apply { add(img, lParams(wrapContent, wrapContent) { centerInParent() }) }
    }
}
