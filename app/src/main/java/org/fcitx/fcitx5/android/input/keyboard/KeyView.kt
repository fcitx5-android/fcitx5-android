package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.*
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

abstract class KeyView(ctx: Context, val theme: Theme, val def: KeyDef.Appearance) :
    CustomGestureView(ctx) {

    val bordered = ThemeManager.prefs.keyBorder.getValue()
    val radius = dp(ThemeManager.prefs.keyRadius.getValue().toFloat())
    val hMargin = dp(ThemeManager.prefs.keyHorizontalMargin.getValue())
    val vMargin = dp(ThemeManager.prefs.keyVerticalMargin.getValue())

    val layout = constraintLayout {
        // sync any state from parent
        isDuplicateParentStateEnabled = true
    }

    init {
        // trigger setEnabled(true)
        isEnabled = true
        isClickable = true
        isHapticFeedbackEnabled = false
        if (def.viewId > 0) {
            id = def.viewId
        }
        // key border
        if (bordered) {
            background = LayerDrawable(
                arrayOf(
                    GradientDrawable().apply {
                        cornerRadius = radius
                        setColor(theme.keyShadowColor.color)
                    },
                    GradientDrawable().apply {
                        cornerRadius = radius
                        setColor(
                            when (def.variant) {
                                Variant.Normal, Variant.AltForeground -> theme.keyBackgroundColor
                                Variant.Alternative -> theme.altKeyBackgroundColor
                                Variant.Accent -> theme.accentKeyBackgroundColor
                            }.color
                        )
                    }
                )
            ).apply {
                val shadowWidth = dp(1)
                setLayerInset(0, hMargin, vMargin, hMargin, vMargin - shadowWidth)
                setLayerInset(1, hMargin, vMargin, hMargin, vMargin)
            }
        } else if (def.forceBordered) {
            // special background
            when (def.viewId) {
                R.id.button_space -> {
                    val hPadding = dp(10)
                    val vPadding = dp(16)
                    background = InsetDrawable(
                        GradientDrawable().apply {
                            cornerRadius = dp(3f)
                            setColor(theme.spaceBarColor.color)
                        },
                        hPadding, vPadding, hPadding, vPadding
                    )
                }
                R.id.button_return -> {
                    // background drawable has no ScaleType support, use an ImageView instead ...
                    val img = imageView {
                        imageDrawable = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setSize(dp(35), dp(35))
                            setColor(theme.accentKeyBackgroundColor.color)
                        }
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                    add(img, lParams(matchParent, matchParent))
                }
            }
            // TODO set press highlight mask for special background keys
        }
        // press highlight
        foreground = if (ThemeManager.prefs.keyRippleEffect.getValue())
            RippleDrawable(
                ColorStateList.valueOf(theme.keyPressHighlightColor.color), null,
                // ripple should be masked with an opaque color
                highlightMaskDrawable(Color.WHITE)
            )
        else
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_pressed),
                    // use mask drawable as highlight directly
                    highlightMaskDrawable(theme.keyPressHighlightColor.color)
                )
            }
        add(layout, lParams(matchParent, matchParent))
    }

    private fun highlightMaskDrawable(@ColorInt color: Int) =
        InsetDrawable(
            if (bordered) GradientDrawable().apply {
                cornerRadius = radius
                setColor(color)
            } else ColorDrawable(color),
            hMargin, vMargin, hMargin, vMargin
        )

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
                Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }.color
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
        // TODO hardcoded alt text size
        textSize = 10.7f
        text = def.altText
        // TODO darken altText color
        setTextColor(
            when (def.variant) {
                Variant.Normal, Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }.color
        )
    }

    init {
        layout.apply { add(altText, lParams(wrapContent, wrapContent)) }
        applyLayout(resources.configuration.orientation)
    }

    private fun applyLayout(orientation: Int) = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            mainText.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomOfParent() }
            altText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topOfParent(vMargin)
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                startToStart = ConstraintLayout.LayoutParams.UNSET
                endOfParent(hMargin + dp(4))
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
                Variant.Normal -> theme.keyTextColor
                Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }.color,
            PorterDuff.Mode.SRC_IN
        )
    }

    init {
        layout.apply { add(img, lParams(wrapContent, wrapContent) { centerInParent() }) }
    }
}
