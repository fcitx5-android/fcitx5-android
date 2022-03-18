package org.fcitx.fcitx5.android.input.editing

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource

class TextEditingUi(override val ctx: Context) : Ui {

    private fun View.borderedBackground() = StateListDrawable().apply {
        setExitFadeDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
            color = styledColorSL(android.R.attr.colorButtonNormal)
            setStroke(dp(1) / 2, styledColorSL(android.R.attr.colorButtonNormal))
        })
        addState(intArrayOf(android.R.attr.state_enabled), GradientDrawable().apply {
            color = styledColorSL(android.R.attr.colorBackground)
            setStroke(dp(1) / 2, styledColorSL(android.R.attr.colorButtonNormal))
        })
    }

    private fun textButton(str: String) = button {
        text = str
        stateListAnimator = null
        background = borderedBackground()
    }

    private fun iconButton(@DrawableRes icon: Int) = imageButton {
        imageResource = icon
        background = borderedBackground()
    }

    var upButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_up_24)

    var rightButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_right_24)

    var downButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_down_24)

    var leftButton = iconButton(R.drawable.ic_baseline_keyboard_arrow_left_24)

    var selectButton = textButton(ctx.getString(R.string.select))

    var homeButton = iconButton(R.drawable.ic_baseline_first_page_24)

    var endButton = iconButton(R.drawable.ic_baseline_last_page_24)

    var selectAllButton = textButton(ctx.getString(android.R.string.selectAll))

    var cutButton = textButton(ctx.getString(android.R.string.cut)).apply { visibility = View.GONE }

    var copyButton = textButton(ctx.getString(android.R.string.copy))

    var pasteButton = textButton(ctx.getString(android.R.string.paste))

    var backspaceButton = iconButton(R.drawable.ic_baseline_backspace_24)

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
                setTextColor(styledColor(android.R.attr.colorForegroundInverse))
                background = styledDrawable(android.R.attr.colorAccent)
            }
        } else {
            selectButton.apply {
                setTextColor(styledColor(android.R.attr.colorForeground))
                background = borderedBackground()
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
        background = styledDrawable(android.R.attr.actionBarItemBackground)
        imageResource = R.drawable.ic_clipboard
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val extension = horizontalLayout {
        add(clipboardButton, lParams(dp(40), dp(40)))
    }
}
