package org.fcitx.fcitx5.android.input.editing

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.*
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import splitties.dimensions.dp
import splitties.resources.color
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.imageResource

class TextEditingUi(override val ctx: Context) : Ui {
    private fun Theme.setForegroundAndBackground(view: View) {
        view.apply {
            if (ThemeManager.prefs.keyRippleEffect.getValue()) {
                foreground = styledDrawable(android.R.attr.selectableItemBackground)
                background = GradientDrawable().apply {
                    setColor(color(android.R.color.transparent))
                    setStroke(dp(1) / 2, ThemeManager.currentTheme.dividerColor.resolve(context))
                }
            } else {
                foreground = null
                background = StateListDrawable().apply {
                    setExitFadeDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
                    addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                        setColor(keyAccentForeground.resolve(context))
                        setStroke(
                            dp(1) / 2,
                            ThemeManager.currentTheme.dividerColor.resolve(context)
                        )
                    })
                    addState(intArrayOf(android.R.attr.state_enabled), GradientDrawable().apply {
                        setColor(color(android.R.color.transparent))
                        setStroke(
                            dp(1) / 2,
                            ThemeManager.currentTheme.dividerColor.resolve(context)
                        )
                    })
                }
            }
        }
    }

    class GTextButton(context: Context) : CustomGestureView(context) {
        val text = textView {
            isClickable = false
            isFocusable = false
            background = null
            ThemeManager.currentTheme.applyKeyTextColor(this)
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
        stateListAnimator = null
        ThemeManager.currentTheme.setForegroundAndBackground(this)
    }

    private fun iconButton(@DrawableRes icon: Int) = GImageButton(ctx).apply {
        image.imageResource = icon
        image.colorFilter =
            ThemeManager.currentTheme.funKeyColor
                .toColorFilter(PorterDuff.Mode.SRC_IN)
                .resolve(context)
        ThemeManager.currentTheme.setForegroundAndBackground(this)
    }

    val upButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_up_24)

    val rightButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_right_24)

    val downButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_down_24)

    val leftButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_left_24)

    val selectButton = textButton(ctx.getString(R.string.select))

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
        if (hasSelection || userSelection) {
            selectButton.apply {
                text.setTextColor(ThemeManager.currentTheme.keyTextColorInverse.resolve(context))
                ThemeManager.currentTheme.applyKeyAccentBackgroundColor(this)
            }
        } else {
            selectButton.apply {
                ThemeManager.currentTheme.applyKeyTextColor(text)
                ThemeManager.currentTheme.setForegroundAndBackground(this)
            }
        }
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
        ThemeManager.currentTheme.applyBarIconColor(this)
        imageResource = R.drawable.ic_clipboard
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val extension = horizontalLayout {
        add(clipboardButton, lParams(dp(40), dp(40)))
    }
}
