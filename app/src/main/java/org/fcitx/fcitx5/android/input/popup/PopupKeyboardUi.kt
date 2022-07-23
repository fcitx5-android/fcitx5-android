package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityCenter

class PopupKeyboardUi(
    override val ctx: Context,
    private val theme: Theme,
    private val radius: Float,
    private val keyboard: KeyDef.Popup.Keyboard
) : Ui {

    private val inactiveBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.keyBackgroundColor.color)
    }

    private val focusBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.accentKeyBackgroundColor.color)
    }

    val keyViews = keyboard.keys.map {
        textView {
            text = it.str
            textSize = 23f
            gravity = gravityCenter
            setTextColor(theme.keyTextColor.color)
            background = inactiveBackground
        }
    }

    var focusedIndex = 0

    // TODO: grid layout
    override val root = horizontalLayout {
        background = inactiveBackground
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(2f)
        markFocus(focusedIndex)
        keyViews.forEach {
            add(it, lParams(dp(38), dp(48)))
        }
    }

    private fun markFocus(index: Int) {
        if (index < 0 || index >= keyViews.size) return
        keyViews[index].apply {
            background = focusBackground
            setTextColor(theme.accentKeyTextColor.color)
        }
    }

    private fun markInactive(index: Int) {
        if (index < 0 || index >= keyViews.size) return
        keyViews[index].apply {
            background = inactiveBackground
            setTextColor(theme.keyTextColor.color)
        }
    }

    fun moveFocus(deltaX: Int, deltaY: Int) {
        markInactive(focusedIndex)
        focusedIndex = (focusedIndex + (deltaX % keyViews.size) + keyViews.size) % keyViews.size
        markFocus(focusedIndex)
    }

    fun trigger(): KeyAction? {
        if (focusedIndex < 0 || focusedIndex >= keyboard.keys.size) return null
        return keyboard.keys[focusedIndex].action
    }
}
