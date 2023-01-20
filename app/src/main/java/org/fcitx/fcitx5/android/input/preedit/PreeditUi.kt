package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.Spannable
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

    fun update(preedit: FcitxEvent.PreeditEvent.Data, aux: FcitxEvent.InputPanelAuxEvent.Data) {
        val bkgColor = theme.genericActiveBackgroundColor
        val upText: SpannedString
        val upCursor: Int
        if (aux.auxUp.isEmpty()) {
            upText = preedit.preedit.toSpannedString(bkgColor)
            upCursor = preedit.preedit.cursor
        } else {
            upText = buildSpannedString {
                append(aux.auxUp.toSpannedString(bkgColor))
                append(preedit.preedit.toSpannedString(bkgColor))
            }
            upCursor = preedit.preedit.cursor.let {
                if (it < 0) it
                else aux.auxUp.length + it
            }
        }
        val downString = aux.auxDown.toSpannedString(bkgColor)
        val hasUp = upText.isNotEmpty()
        val hasDown = downString.isNotEmpty()
        visible = hasUp || hasDown
        if (!visible) return
        val upString = if (upCursor < 0 || upCursor == upText.length) {
            upText
        } else buildSpannedString {
            append(upText, 0, upCursor)
            append('|')
            setSpan(cursorSpan, upCursor, upCursor + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(upText, upCursor, upText.length)
        }
        updateTextView(upView, upString, hasUp)
        updateTextView(downView, downString, hasDown)
    }
}
