/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import android.widget.TextView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.gravityCenter

class PopupEntryUi(override val ctx: Context, private val theme: Theme, keyHeight: Int, radius: Float) : Ui {

    var lastShowTime = -1L

    val textView = view(::AutoScaleTextView) {
        textSize = 23f
        gravity = gravityCenter
        setTextColor(theme.popupTextColor)
    }

    override val root = constraintLayout {
        background = GradientDrawable().apply {
            cornerRadius = radius
            setColor(theme.popupBackgroundColor)
        }
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(2f)
        add(textView, lParams(matchParent, keyHeight) {
            topOfParent()
            centerHorizontally()
        })
    }

    fun setText(text: String) {
        textView.text = text
    }

    fun setTextHeight(heightPx: Int) {
        val lp = textView.layoutParams
        if (lp != null && lp.height != heightPx) {
            lp.height = heightPx
            textView.layoutParams = lp
        }
    }

    fun setDangerHint(highlight: Boolean) {
        val bg = (root.background as? GradientDrawable) ?: return
        if (highlight) {
            bg.setColor(0xFFE53935.toInt())
            textView.setTextColor(0xFFFFFFFF.toInt())
        } else {
            bg.setColor(theme.popupBackgroundColor)
            textView.setTextColor(theme.popupTextColor)
        }
    }

    fun resetForReuse() {
        // restore visual defaults to avoid leaking state across pooled instances
        setDangerHint(false)
        textView.text = ""
    }
}
