package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding

class PreeditUi(override val ctx: Context) : Ui {

    private val beforeCursor = textView {
        textSize = 16f
    }

    private val cursorView = view(::View) {
        backgroundColor = styledColor(android.R.attr.colorControlNormal)
    }

    private val afterCursor = textView {
        textSize = 16f
    }

    private val upView = horizontalLayout {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        horizontalPadding = dp(8)
        add(beforeCursor, lParams())
        add(cursorView, lParams(dp(1), matchParent) { verticalMargin = dp(2) })
        add(afterCursor, lParams())
    }

    private val downView = textView {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        horizontalPadding = dp(8)
        textSize = 16f
    }

    var visible = false
        private set

    override val root: View = verticalLayout {
        alpha = 0.8f
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

    fun measureHeight(width: Int = 0): Int {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        return root.measuredHeight
    }
}
