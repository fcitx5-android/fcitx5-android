package me.rocka.fcitx5test.input.candidates.adapter

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.dimen
import splitties.resources.dimenPxSize
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.horizontalPadding

abstract class SimpleCandidateViewAdapter : BaseCandidateViewAdapter() {
    override fun createTextView(parent: ViewGroup): TextView =
        parent.textView {
            layoutParams = ViewGroup.LayoutParams(wrapContent, dp(40))
            gravity = gravityCenter
            setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen(R.dimen.candidate_font_size))
            horizontalPadding = dimenPxSize(R.dimen.candidate_padding)
            minWidth = dimenPxSize(R.dimen.candidate_min_width)
            isSingleLine = true
        }
}