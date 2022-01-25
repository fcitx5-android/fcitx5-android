package me.rocka.fcitx5test.keyboard

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
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

    val before = createTextView()

    val after = createTextView()

    override val root: View = view(::RelativeLayout) {
        alpha = 0.8f
        add(before, ViewGroup.LayoutParams(wrapContent, wrapContent))
        add(after, ViewGroup.LayoutParams(wrapContent, wrapContent))
    }
}
