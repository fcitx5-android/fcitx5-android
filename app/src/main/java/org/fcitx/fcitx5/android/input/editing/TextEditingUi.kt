package org.fcitx.fcitx5.android.input.editing

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.borderDrawable
import org.fcitx.fcitx5.android.utils.borderlessRippleDrawable
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.imageResource

class TextEditingUi(override val ctx: Context, private val theme: Theme) : Ui {
    private val borderWidth = ctx.dp(1) / 2

    private fun View.applyBorderedBackground() {
        background = borderDrawable(borderWidth, theme.dividerColor)
        foreground =
            if (ThemeManager.prefs.keyRippleEffect.getValue())
                rippleDrawable(theme.keyPressHighlightColor)
            else pressHighlightDrawable(theme.keyPressHighlightColor)
    }

    class GTextButton(context: Context) : CustomGestureView(context) {
        val text = textView {
            isClickable = false
            isFocusable = false
            background = null
        }

        init {
            add(text, lParams(wrapContent, wrapContent, gravityCenter))
        }
    }

    class GImageButton(context: Context) : CustomGestureView(context) {
        val image = imageView {
            isClickable = false
            isFocusable = false
        }

        init {
            add(image, lParams(wrapContent, wrapContent, gravityCenter))
        }
    }

    private fun textButton(str: String) = GTextButton(ctx).apply {
        text.text = str
        text.setTextColor(theme.keyTextColor)
        stateListAnimator = null
        applyBorderedBackground()
    }

    private fun iconButton(@DrawableRes icon: Int) = GImageButton(ctx).apply {
        image.imageResource = icon
        image.colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        applyBorderedBackground()
    }

    val upButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_up_24)

    val rightButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_right_24)

    val downButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_down_24)

    val leftButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_left_24)

    val selectButton = textButton(ctx.getString(R.string.select)).apply {
        text.setTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_activated),
                    intArrayOf(android.R.attr.state_enabled)
                ),
                intArrayOf(theme.accentKeyTextColor, theme.keyTextColor)
            )
        )
        background = StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_activated),
                borderDrawable(borderWidth, theme.dividerColor, theme.accentKeyBackgroundColor)
            )
            addState(
                intArrayOf(android.R.attr.state_enabled),
                borderDrawable(borderWidth, theme.dividerColor)
            )
        }
    }

    val homeButton = iconButton(R.drawable.ic_baseline_first_page_24)

    val endButton = iconButton(R.drawable.ic_baseline_last_page_24)

    val selectAllButton = textButton(ctx.getString(android.R.string.selectAll))

    val cutButton = textButton(ctx.getString(android.R.string.cut)).apply { visibility = View.GONE }

    val copyButton = textButton(ctx.getString(android.R.string.copy))

    val pasteButton = textButton(ctx.getString(android.R.string.paste))

    val backspaceButton = iconButton(R.drawable.ic_baseline_backspace_24)

    override val root = constraintLayout {
        add(leftButton, lParams {
            topOfParent()
            startOfParent()
            above(homeButton)
            before(selectButton)
        })
        add(upButton, lParams {
            topOfParent()
            after(leftButton)
            above(selectButton)
            before(rightButton)
        })
        add(selectButton, lParams {
            below(upButton)
            after(leftButton)
            above(downButton)
            before(rightButton)
        })
        add(downButton, lParams {
            below(selectButton)
            after(leftButton)
            above(homeButton)
            before(rightButton)
        })
        add(rightButton, lParams {
            topOfParent()
            after(selectButton)
            above(endButton)
            before(copyButton)
        })

        add(homeButton, lParams {
            below(downButton)
            startOfParent()
            bottomOfParent()
            before(endButton)
        })
        add(endButton, lParams {
            below(downButton)
            after(homeButton)
            bottomOfParent()
            before(backspaceButton)
        })

        add(selectAllButton, lParams {
            topOfParent()
            after(rightButton)
            endOfParent()
            above(cutButton)
            matchConstraintPercentWidth = 0.3f
        })
        add(cutButton, lParams {
            below(selectAllButton)
            after(rightButton)
            endOfParent()
            above(copyButton)
            matchConstraintPercentWidth = 0.3f
        })
        add(copyButton, lParams {
            below(cutButton)
            after(rightButton)
            endOfParent()
            above(pasteButton)
            matchConstraintPercentWidth = 0.3f
        })
        add(pasteButton, lParams {
            below(copyButton)
            after(rightButton)
            endOfParent()
            above(backspaceButton)
            matchConstraintPercentWidth = 0.3f
        })
        add(backspaceButton, lParams {
            below(pasteButton)
            after(rightButton)
            endOfParent()
            bottomOfParent()
            matchConstraintPercentWidth = 0.3f
        })
    }

    fun updateSelection(hasSelection: Boolean, userSelection: Boolean) {
        selectButton.isActivated = (hasSelection || userSelection)
        if (hasSelection) {
            selectAllButton.apply {
                visibility = View.GONE
            }
            cutButton.apply {
                visibility = View.VISIBLE
            }
        } else {
            selectAllButton.apply {
                visibility = View.VISIBLE
            }
            cutButton.apply {
                visibility = View.GONE
            }
        }
    }

    val clipboardButton = imageButton {
        background = borderlessRippleDrawable(theme.keyPressHighlightColor, dp(20))
        colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        imageResource = R.drawable.ic_clipboard
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val extension = horizontalLayout {
        add(clipboardButton, lParams(dp(40), dp(40)))
    }
}
