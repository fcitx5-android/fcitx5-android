package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeManager.Prefs.PunctuationPosition
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.utils.styledFloat
import org.fcitx.fcitx5.android.utils.unset
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerInParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.parentId
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.existingOrNewId
import splitties.views.imageResource
import splitties.views.padding
import kotlin.math.min
import kotlin.math.roundToInt

abstract class KeyView(ctx: Context, val theme: Theme, val def: KeyDef.Appearance) :
    CustomGestureView(ctx) {

    val bordered: Boolean
    val rippled: Boolean
    val radius: Float
    val hMargin: Int
    val vMargin: Int

    init {
        val prefs = ThemeManager.prefs
        bordered = prefs.keyBorder.getValue()
        rippled = prefs.keyRippleEffect.getValue()
        radius = dp(prefs.keyRadius.getValue().toFloat())
        hMargin = if (def.margin) dp(prefs.keyHorizontalMargin.getValue()) else 0
        vMargin = if (def.margin) dp(prefs.keyVerticalMargin.getValue()) else 0
    }

    private val cachedLocation = intArrayOf(0, 0)
    private val cachedBounds = Rect()
    private var boundsValid = false
    val bounds: Rect
        get() = cachedBounds.also {
            if (!boundsValid) updateBounds()
        }

    /**
     * KeyView content left margin, in percentage of parent width
     */
    @FloatRange(0.0, 1.0)
    var layoutMarginLeft = 0f

    /**
     * KeyView content right margin, in percentage of parent width
     */
    @FloatRange(0.0, 1.0)
    var layoutMarginRight = 0f

    /**
     * [KeyView] contains 2 parts: `TouchEventView` and `AppearanceView`.
     *
     * `TouchEventView` is the outer [CustomGestureView] that handles touch events.
     *
     * `AppearanceView` in the inner [ConstraintLayout], it can be smaller than its parent,
     * and holds the [bounds] for popup.
     */
    protected val appearanceView = constraintLayout {
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
            appearanceView.background = LayerDrawable(
                arrayOf(
                    GradientDrawable().apply {
                        cornerRadius = radius
                        setColor(theme.keyShadowColor)
                    },
                    GradientDrawable().apply {
                        cornerRadius = radius
                        setColor(
                            when (def.variant) {
                                Variant.Normal, Variant.AltForeground -> theme.keyBackgroundColor
                                Variant.Alternative -> theme.altKeyBackgroundColor
                                Variant.Accent -> theme.accentKeyBackgroundColor
                            }
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
        add(appearanceView, lParams(matchParent, matchParent))
    }

    private fun setupPressHighlight(mask: Drawable? = null) {
        appearanceView.foreground = if (rippled)
            RippleDrawable(
                ColorStateList.valueOf(theme.keyPressHighlightColor), null,
                // ripple should be masked with an opaque color
                mask ?: highlightMaskDrawable(Color.WHITE)
            )
        else
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_pressed),
                    // use mask drawable as highlight directly
                    mask ?: highlightMaskDrawable(theme.keyPressHighlightColor)
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
        appearanceView.alpha = if (enabled) 1f else styledFloat(android.R.attr.disabledAlpha)
    }

    fun updateBounds() {
        val (x, y) = cachedLocation.also { appearanceView.getLocationInWindow(it) }
        cachedBounds.set(x, y, x + appearanceView.width, y + appearanceView.height)
        boundsValid = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        boundsValid = false
        if (layoutMarginLeft != 0f || layoutMarginRight != 0f) {
            val w = right - left
            val h = bottom - top
            val layoutWidth = (w * (1f - layoutMarginLeft - layoutMarginRight)).roundToInt()
            appearanceView.updateLayoutParams<LayoutParams> {
                leftMargin = (w * layoutMarginLeft).roundToInt()
                rightMargin = (w * layoutMarginRight).roundToInt()
            }
            // sets `measuredWidth` and `measuredHeight` of `AppearanceView`
            // https://developer.android.com/guide/topics/ui/how-android-draws#measure
            appearanceView.measure(
                MeasureSpec.makeMeasureSpec(layoutWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            )
        }
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (bordered) return
        when (def.viewId) {
            R.id.button_space -> {
                val bkgRadius = dp(3f)
                val minHeight = dp(26)
                val hInset = dp(10)
                val vInset = if (h < minHeight) 0 else min((h - minHeight) / 2, dp(16))
                appearanceView.background = InsetDrawable(
                    GradientDrawable().apply {
                        cornerRadius = bkgRadius
                        setColor(theme.spaceBarColor)
                    },
                    hInset, vInset, hInset, vInset
                )
                // InsetDrawable sets padding to container view; remove padding to prevent text from bing clipped
                appearanceView.padding = 0
                // apply press highlight for background area
                setupPressHighlight(
                    InsetDrawable(
                        GradientDrawable().apply {
                            cornerRadius = bkgRadius
                            setColor(if (rippled) Color.WHITE else theme.keyPressHighlightColor)
                        },
                        hInset, vInset, hInset, vInset
                    )
                )
            }
            R.id.button_return -> {
                val drawableSize = min(min(w, h), dp(35))
                val hInset = (w - drawableSize) / 2
                val vInset = (h - drawableSize) / 2
                appearanceView.background = InsetDrawable(
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(theme.accentKeyBackgroundColor)
                    },
                    hInset, vInset, hInset, vInset
                )
                appearanceView.padding = 0
                setupPressHighlight(
                    InsetDrawable(
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (rippled) Color.WHITE else theme.keyPressHighlightColor)
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
    val mainText = view(::AutoScaleTextView) {
        isClickable = false
        isFocusable = false
        background = null
        text = def.displayText
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, def.textSize)
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG_LTR
        // keep original typeface, apply textStyle only
        setTypeface(typeface, def.textStyle)
        setTextColor(
            when (def.variant) {
                Variant.Normal -> theme.keyTextColor
                Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }
        )
    }

    init {
        appearanceView.apply {
            add(mainText, lParams(wrapContent, wrapContent) {
                centerInParent()
            })
        }
    }
}

@SuppressLint("ViewConstructor")
class AltTextKeyView(ctx: Context, theme: Theme, def: KeyDef.Appearance.AltText) :
    TextKeyView(ctx, theme, def) {
    val altText = view(::AutoScaleTextView) {
        isClickable = false
        isFocusable = false
        // TODO hardcoded alt text size
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.666667f)
        setTypeface(typeface, Typeface.BOLD)
        text = def.altText
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG_LTR
        setTextColor(
            when (def.variant) {
                Variant.Normal, Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
                Variant.Accent -> theme.accentKeyTextColor
            }
        )
    }

    init {
        appearanceView.apply {
            add(altText, lParams(wrapContent, wrapContent))
        }
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
            leftToLeft = unset
            rightToRight = parentId; rightMargin = hMargin + dp(4)
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
            rightMargin = 0
            // set
            leftToLeft = parentId
            rightToRight = parentId
            bottomToBottom = parentId; bottomMargin = vMargin + dp(2)
        }
    }

    private fun applyLayout(orientation: Int) {
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
    val img = imageView { configure(theme, def.src, def.variant) }

    init {
        appearanceView.apply {
            add(img, lParams(wrapContent, wrapContent) {
                centerInParent()
            })
        }
    }
}

private fun ImageView.configure(theme: Theme, @DrawableRes src: Int, variant: Variant) = apply {
    isClickable = false
    isFocusable = false
    imageTintList = ColorStateList.valueOf(
        when (variant) {
            Variant.Normal -> theme.keyTextColor
            Variant.AltForeground, Variant.Alternative -> theme.altKeyTextColor
            Variant.Accent -> theme.accentKeyTextColor
        }
    )
    imageResource = src
}

@SuppressLint("ViewConstructor")
class ImageTextKeyView(ctx: Context, theme: Theme, def: KeyDef.Appearance.ImageText) :
    TextKeyView(ctx, theme, def) {
    val img = imageView {
        configure(theme, def.src, def.variant)
    }

    init {
        appearanceView.apply {
            add(img, lParams(dp(13), dp(13)))
        }
        mainText.updateLayoutParams<ConstraintLayout.LayoutParams> {
            centerHorizontally()
            bottomToBottom = parentId
            bottomMargin = vMargin + dp(4)
            topToTop = unset
        }
        img.updateLayoutParams<ConstraintLayout.LayoutParams> {
            centerHorizontally()
            topToTop = parentId
        }
        updateMargins(resources.configuration.orientation)
    }

    private fun updateMargins(orientation: Int) {
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                mainText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomMargin = vMargin + dp(2)
                }
                img.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = vMargin + dp(4)
                }
            }
            else -> {
                mainText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomMargin = vMargin + dp(4)
                }
                img.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = vMargin + dp(8)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateMargins(newConfig.orientation)
    }
}
