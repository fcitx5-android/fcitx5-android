package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.view.View
import android.view.View.*
import android.widget.TextView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding

class PreeditUi(override val ctx: Context, private val inputTheme: Theme) : Ui {

    private val beforeCursor = textView {
        textSize = 16f
        setTextColor(inputTheme.keyTextColor.color)
    }

    private val cursorView = view(::View) {
        backgroundColor = inputTheme.keyTextColor.color
    }

    private val afterCursor = textView {
        textSize = 16f
        setTextColor(inputTheme.keyTextColor.color)
    }

    private val barBackground = when (inputTheme) {
        is Theme.Builtin ->
            if (ThemeManager.prefs.keyBorder.getValue()) inputTheme.backgroundColor
            else inputTheme.barColor
        is Theme.Custom -> inputTheme.backgroundColor
    }

    private val upView = horizontalLayout {
        backgroundColor = barBackground.color
        horizontalPadding = dp(8)
        add(beforeCursor, lParams())
        add(cursorView, lParams(dp(1), matchParent) { verticalMargin = dp(2) })
        add(afterCursor, lParams())
    }

    private val downView = textView {
        backgroundColor = barBackground.color
        horizontalPadding = dp(8)
        textSize = 16f
    }

    var visible = false
        private set

    override val root: View = verticalLayout {
        alpha = 0.8f
        visibility = INVISIBLE
        add(upView, lParams())
        add(downView, lParams())
    }

    private fun updateTextView(view: TextView, str: String, visible: Boolean) = view.run {
        if (visible) {
            text = str
            if (visibility == GONE) visibility = VISIBLE
        } else if (visibility != GONE) {
            visibility = GONE
        }
    }

    fun update(content: PreeditContent) {
        val beforeText: String
        val afterText: String
        if (content.preedit.cursor >= 0) {
            beforeText = content.aux.auxUp + content.preedit.run { preedit.take(cursor) }
            afterText = content.preedit.run { preedit.drop(cursor) }
        } else {
            beforeText = content.aux.auxUp + content.preedit.preedit
            afterText = ""
        }
        val downText = content.aux.auxDown
        val hasBefore = beforeText.isNotEmpty()
        val hasAfter = afterText.isNotEmpty()
        val hasDown = downText.isNotEmpty()
        visible = hasBefore || hasAfter || hasDown
        if (!visible) return
        cursorView.visibility = if (hasAfter) VISIBLE else GONE
        updateTextView(beforeCursor, beforeText, hasBefore)
        updateTextView(afterCursor, afterText, hasAfter)
        updateTextView(downView, downText, hasDown)
    }
}
