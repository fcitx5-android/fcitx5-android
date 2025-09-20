/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import android.widget.ImageView
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
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.view
import splitties.views.gravityCenter

class PopupEntryUi(override val ctx: Context, private val theme: Theme, keyHeight: Int, radius: Float) : Ui {

    var lastShowTime = -1L
    private var iconMode = false

    val textView = view(::AutoScaleTextView) {
        textSize = 23f
        gravity = gravityCenter
        setTextColor(theme.popupTextColor)
    }

    private val iconView = imageView {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        adjustViewBounds = true
        imageTintList = ColorStateList.valueOf(theme.popupTextColor)
        // leave some breathing room around the icon
        val pad = dp(6)
        setPadding(pad, 0, pad, 0)
        visibility = android.view.View.GONE
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
        add(iconView, lParams(matchParent, keyHeight) {
            topOfParent()
            centerHorizontally()
        })
    }

    fun setText(text: String) {
        if (!iconMode) textView.text = text
    }

    fun setTextHeight(heightPx: Int) {
        val lp = textView.layoutParams
        if (lp != null && lp.height != heightPx) {
            lp.height = heightPx
            textView.layoutParams = lp
        }
        val lp2 = iconView.layoutParams
        if (lp2 != null && lp2.height != heightPx) {
            lp2.height = heightPx
            iconView.layoutParams = lp2
        }
    }

    fun setArmed(armed: Boolean) {
        val bg = (root.background as? GradientDrawable) ?: return
        if (armed) {
            bg.setColor(0xFFE53935.toInt())
            textView.setTextColor(0xFFFFFFFF.toInt())
            iconView.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        } else {
            bg.setColor(theme.popupBackgroundColor)
            textView.setTextColor(theme.popupTextColor)
            iconView.imageTintList = ColorStateList.valueOf(theme.popupTextColor)
        }
    }

    fun showIcon(resId: Int) {
        if (!iconMode) {
            iconMode = true
            textView.visibility = android.view.View.GONE
            iconView.visibility = android.view.View.VISIBLE
        }
        iconView.setImageResource(resId)
    }

    fun showTextMode() {
        if (iconMode) {
            iconMode = false
            iconView.visibility = android.view.View.GONE
            textView.visibility = android.view.View.VISIBLE
        }
    }

    fun resetForReuse() {
        // restore visual defaults to avoid leaking state across pooled instances
        setArmed(false)
        showTextMode()
        textView.text = ""
        iconView.setImageDrawable(null)
    }
}
