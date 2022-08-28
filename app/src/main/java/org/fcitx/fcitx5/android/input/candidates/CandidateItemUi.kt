package org.fcitx.fcitx5.android.input.candidates

import android.content.Context
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.horizontalPadding

class CandidateItemUi(override val ctx: Context, theme: Theme) : Ui {

    companion object {
        val systemTouchSounds by AppPrefs.getInstance().keyboard.systemTouchSounds
    }

    val text = textView {
        textSize = 20f // sp
        isSingleLine = true
        gravity = gravityCenter
        setTextColor(theme.keyTextColor.color)
    }

    override val root = view(::CustomGestureView) {
        minimumWidth = dp(40)
        horizontalPadding = dp(8)
        isSoundEffectsEnabled = systemTouchSounds
        background = pressHighlightDrawable(theme.keyPressHighlightColor.color)

        add(text, lParams(wrapContent, matchParent) {
            gravity = gravityCenter
        })
    }
}
