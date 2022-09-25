package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.*
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeManager.Prefs.PunctuationPosition
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.utils.styledFloat
import org.fcitx.fcitx5.android.utils.unset
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.centerInParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.parentId
import splitties.views.dsl.core.*
import splitties.views.existingOrNewId
import splitties.views.imageDrawable
import splitties.views.padding
import kotlin.math.min

abstract class KeyView(ctx: Context, val theme: Theme, val def: KeyDef.Appearance) :
    CustomGestureView(ctx) {

    val bordered = ThemeManager.prefs.keyBorder.getValue()
    val rippled = ThemeManager.prefs.keyRippleEffect.getValue()
    val radius = dp(ThemeManager.prefs.keyRadius.getValue().toFloat())
    val hMargin = dp(ThemeManager.prefs.keyHorizontalMargin.getValue())
    val vMargin = dp(ThemeManager.prefs.keyVerticalMargin.getValue())

    val bounds = Rect()

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
        if ((bordered && def.border != Border.Off) || def.border == Border.On) {
            // background: key border
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
            // foreground: press highlight or ripple
            setupPressHighlight()
        } else {
            // normal press highlight for keys without special background
            // special background is handled in `onSizeChanged()`
            if (def.border != Border.Special) {
                setupPressHighlight()
            }
        }
        add(layout, lParams(matchParent, matchParent))
    }

    private fun setupPressHighlight(mask: Drawable? = null) {
        foreground = if (rippled)
            RippleDrawable(
                ColorStateList.valueOf(theme.keyPressHighlightColor.color), null,
                // ripple should be masked with an opaque color
                mask ?: highlightMaskDrawable(Color.WHITE)
            )
        else
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_pressed),
                    // use mask drawable as highlight directly
                    mask ?: highlightMaskDrawable(theme.keyPressHighlightColor.color)
                )
            }
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val (x, y) = intArrayOf(0, 0).also { getLocationInWindow(it) }
        bounds.set(x, y, x + w, y + h)
        if (bordered) return
        when (def.viewId) {
            R.id.button_space -> {
                val bkgRadius = dp(3f)
                val minHeight = dp(26)
                val hInset = dp(10)
                val vInset = if (h < minHeight) 0 else min((h - minHeight) / 2, dp(16))
                background = InsetDrawable(
                    GradientDrawable().apply {
                        cornerRadius = bkgRadius
                        setColor(theme.spaceBarColor.color)
                    },
                    hInset, vInset, hInset, vInset
                )
                // InsetDrawable sets padding to container view; remove padding to prevent text from bing clipped
                padding = 0
                // apply press highlight for background area
                setupPressHighlight(
                    InsetDrawable(
                        GradientDrawable().apply {
                            cornerRadius = bkgRadius
                            setColor(if (rippled) Color.WHITE else theme.keyPressHighlightColor.color)
                        },
                        hInset, vInset, hInset, vInset
                    )
                )
            }
            R.id.button_return -> {
                val drawableSize = min(min(w, h), dp(35))
                val hInset = (w - drawableSize) / 2
                val vInset = (h - drawableSize) / 2
                background = InsetDrawable(
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(theme.accentKeyBackgroundColor.color)
                    },
                    hInset, vInset, hInset, vInset
                )
                padding = 0
                setupPressHighlight(
                    InsetDrawable(
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (rippled) Color.WHITE else theme.keyPressHighlightColor.color)
                        },
                        hInset, vInset, hInset, vInset
                    )
                )
            }
        }
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
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
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

    private fun applyTopRightAltTextPosition() {
        mainText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            // reset
            topMargin = 0
            bottomToTop = unset
            // set
            topToTop = parentId
            bottomToBottom = parentId
        }
        altText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            // reset
            bottomToBottom = unset; bottomMargin = 0
            // set
            topToTop = parentId; topMargin = vMargin
            startToStart = unset
            endToEnd = parentId; endMargin = hMargin + dp(4)
        }
    }

    private fun applyBottomAltTextPosition() {
        mainText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            // reset
            bottomToBottom = unset
            // set
            topToTop = parentId; topMargin = vMargin
            bottomToTop = altText.existingOrNewId
        }
        altText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            // reset
            topToTop = unset; topMargin = 0
            endMargin = 0
            // set
            startToStart = parentId
            endToEnd = parentId
            bottomToBottom = parentId; bottomMargin = vMargin + dp(2)
        }
    }

    private fun applyLayout(orientation: Int) {
        Configuration.ORIENTATION_PORTRAIT
        when (ThemeManager.prefs.punctuationPosition.getValue()) {
            PunctuationPosition.Bottom -> when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> applyTopRightAltTextPosition()
                else -> applyBottomAltTextPosition()
            }
            PunctuationPosition.TopRight -> applyTopRightAltTextPosition()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (ThemeManager.prefs.punctuationPosition.getValue() == PunctuationPosition.TopRight) {
            return
        }
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
