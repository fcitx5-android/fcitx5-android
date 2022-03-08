package me.rocka.fcitx5test.input.preedit

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding

class PreeditUi(override val ctx: Context) : Ui {
    private fun createTextView() = textView {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        horizontalPadding = dp(8)
        textSize = 16f // sp
    }

    private val upView = createTextView()

    private val downView = createTextView()

    var visible = false
        private set

    override val root: View = verticalLayout {
        alpha = 0.8f
        add(upView, ViewGroup.LayoutParams(wrapContent, wrapContent))
        add(downView, ViewGroup.LayoutParams(wrapContent, wrapContent))
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
        val upText = content.aux.auxUp + content.preedit.preedit
        val downText = content.aux.auxDown
        val hasUp = upText.isNotEmpty()
        val hasDown = downText.isNotEmpty()
        visible = hasUp || hasDown
        if (!visible) return
        updateTextView(upView, upText, hasUp)
        updateTextView(downView, downText, hasDown)
    }

    fun measureHeight(width: Int = 0): Int {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        return root.measuredHeight
    }
}
