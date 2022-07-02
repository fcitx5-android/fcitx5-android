package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.Spannable
import android.text.style.DynamicDrawableSpan
import android.view.View
import android.view.View.*
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding

class PreeditUi(override val ctx: Context, private val inputTheme: Theme) : Ui {

    class CursorSpan(ctx: Context, @ColorInt color: Int, metrics: Paint.FontMetricsInt) :
        DynamicDrawableSpan() {
        private val drawable = ShapeDrawable(RectShape()).apply {
            paint.color = color
            setBounds(0, metrics.ascent, ctx.dp(1), metrics.bottom)
        }

        override fun getDrawable() = drawable
    }

    private val cursorSpan by lazy {
        CursorSpan(ctx, inputTheme.keyTextColor.color, upView.paint.fontMetricsInt)
    }

    private val keyBorder by ThemeManager.prefs.keyBorder

    private val barBackground = when (inputTheme) {
        is Theme.Builtin -> if (keyBorder) inputTheme.backgroundColor else inputTheme.barColor
        is Theme.Custom -> inputTheme.backgroundColor
    }

    private fun createTextView() = textView {
        backgroundColor = barBackground.color
        horizontalPadding = dp(8)
        setTextColor(inputTheme.keyTextColor.color)
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

    fun update(content: PreeditContent) {
        val upText: String
        val upCursor: Int
        if (content.aux.auxUp.isEmpty()) {
            upText = content.preedit.preedit
            upCursor = content.preedit.cursor
        } else {
            upText = content.aux.auxUp + content.preedit.preedit
            upCursor = content.preedit.cursor.let {
                if (it < 0) it
                else content.aux.auxUp.length + it
            }
        }
        val downText = content.aux.auxDown
        val hasUp = upText.isNotEmpty()
        val hasDown = downText.isNotEmpty()
        visible = hasUp || hasDown
        if (!visible) return
        updateTextView(upView, if (upCursor < 0 || upCursor == upText.length) {
            upText
        } else {
            buildSpannedString {
                append(upText, 0, upCursor)
                append('|')
                setSpan(cursorSpan, upCursor, upCursor + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(upText, upCursor, upText.length)
            }
        }, hasUp)
        updateTextView(downView, downText, hasDown)
    }
}
