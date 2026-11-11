/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.gravityCenter
import splitties.views.setPaddingDp

class TokenizedClipboardTokenUi(
    override val ctx: Context,
    private val theme: Theme
) : Ui {

    val textView = textView {
        gravity = gravityCenter
        minLines = 1
        maxLines = 1
        textSize = 15f
        setPaddingDp(12, 8, 12, 8)
    }

    override val root = CustomGestureView(ctx).apply {
        isClickable = true
        minimumHeight = ctx.dp(38)
        add(textView, lParams(matchParent, matchParent))
    }

    fun setToken(text: String, selected: Boolean) {
        textView.text = text
        textView.setTextColor(if (selected) theme.altKeyTextColor else theme.keyTextColor)
        root.foreground = RippleDrawable(
            ColorStateList.valueOf(theme.keyPressHighlightColor),
            null,
            GradientDrawable().apply {
                cornerRadius = ctx.dp(12).toFloat()
                setColor(Color.WHITE)
            }
        )
        root.background = GradientDrawable().apply {
            cornerRadius = ctx.dp(12).toFloat()
            setColor(if (selected) theme.genericActiveBackgroundColor else theme.keyBackgroundColor)
        }
    }
}
