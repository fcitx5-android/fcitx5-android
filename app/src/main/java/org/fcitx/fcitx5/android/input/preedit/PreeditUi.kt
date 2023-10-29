/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.Spanned
import android.text.SpannedString
import android.text.style.DynamicDrawableSpan
import android.view.View
import android.view.View.*
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding

class PreeditUi(override val ctx: Context, private val theme: Theme) : Ui {

    class CursorSpan(ctx: Context, @ColorInt color: Int, metrics: Paint.FontMetricsInt) :
        DynamicDrawableSpan() {
        private val drawable = ShapeDrawable(RectShape()).apply {
            paint.color = color
            setBounds(0, metrics.ascent, ctx.dp(1), metrics.bottom)
        }

        override fun getDrawable() = drawable
    }

    private val cursorSpan by lazy {
        CursorSpan(ctx, theme.keyTextColor, upView.paint.fontMetricsInt)
    }

    private val keyBorder by ThemeManager.prefs.keyBorder

    private val barBackground = when (theme) {
        is Theme.Builtin -> if (keyBorder) theme.backgroundColor else theme.barColor
        is Theme.Custom -> theme.backgroundColor
    }

    private fun createTextView() = textView {
        backgroundColor = barBackground
        horizontalPadding = dp(8)
        setTextColor(theme.keyTextColor)
        textSize = 16f
    }

    private val upView = createTextView()

    private val downView = createTextView()

    var visible = false
        private set

    override val root: View = verticalLayout {
        alpha = 0.8f
        visibility = INVISIBLE
        add(upView, lParams())
        add(downView, lParams())
    }

    private fun updateTextView(view: TextView, str: CharSequence, visible: Boolean) = view.run {
        if (visible) {
            text = str
            if (visibility == GONE) visibility = VISIBLE
        } else if (visibility != GONE) {
            visibility = GONE
        }
    }

    fun update(inputPanel: FcitxEvent.InputPanelEvent.Data) {
        val bkgColor = theme.genericActiveBackgroundColor
        val upString: SpannedString
        val upCursor: Int
        if (inputPanel.auxUp.isEmpty()) {
            upString = inputPanel.preedit.toSpannedString(bkgColor)
            upCursor = inputPanel.preedit.cursor
        } else {
            upString = buildSpannedString {
                append(inputPanel.auxUp.toSpannedString(bkgColor))
                append(inputPanel.preedit.toSpannedString(bkgColor))
            }
            upCursor = inputPanel.preedit.cursor.let {
                if (it < 0) it
                else inputPanel.auxUp.length + it
            }
        }
        val downString = inputPanel.auxDown.toSpannedString(bkgColor)
        val hasUp = upString.isNotEmpty()
        val hasDown = downString.isNotEmpty()
        visible = hasUp || hasDown
        if (!visible) return
        val upStringWithCursor = if (upCursor < 0 || upCursor == upString.length) {
            upString
        } else buildSpannedString {
            if (upCursor > 0) append(upString, 0, upCursor)
            append('|')
            setSpan(cursorSpan, upCursor, upCursor + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            append(upString, upCursor, upString.length)
        }
        updateTextView(upView, upStringWithCursor, hasUp)
        updateTextView(downView, downString, hasDown)
    }
}
