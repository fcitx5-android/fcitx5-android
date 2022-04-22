package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.applyKeyAltTextColor
import org.fcitx.fcitx5.android.data.theme.applyKeyTextColor
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import org.fcitx.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledDrawable
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
        radius = dp(ThemeManager.prefs.keyRadius.getValue())
        cardElevation = 0f
        if (def is KeyDef.Appearance.Text && def.forceBordered) {
            setCardBackgroundColor(theme.keyBackgroundColorBordered.resolve(context))
        } else if (def is KeyDef.Appearance.Image && def.accentBackground)
            setCardBackgroundColor(theme.keyAccentBackgroundColor.resolve(context))
        else if (ThemeManager.prefs.keyBorder.getValue())
            setCardBackgroundColor(theme.keyBackgroundColorBordered.resolve(context))
        else theme.keyBackgroundColor?.let {
            setCardBackgroundColor(it.resolve(context))
        } ?: run { background = null }

        // sync pressed state from parent
        isDuplicateParentStateEnabled = true
        // pressed highlight
        foreground =
            if (ThemeManager.prefs.keyRippleEffect.getValue())
                styledDrawable(android.R.attr.selectableItemBackground)
            else StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                    setColor(theme.keyAccentForeground.resolve(context))
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
        if (def.isFunKey)
            setTextColor(theme.funKeyColor.resolve(context))
        else
            theme.applyKeyTextColor(this)
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
        theme.applyKeyAltTextColor(this)
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
        colorFilter = if (def.accentBackground)
            theme.keyTextColorInverse.toColorFilter(PorterDuff.Mode.SRC_IN).resolve(context)
        else
            theme.funKeyColor.toColorFilter(PorterDuff.Mode.SRC_IN).resolve(context)
    }

    init {
        layout.apply { add(img, lParams(wrapContent, wrapContent) { centerInParent() }) }
    }
}
