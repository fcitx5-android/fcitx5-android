package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.gravityCenter

@SuppressLint("ViewConstructor")
class PickerPageView(context: Context, theme: Theme): ConstraintLayout(context) {

    val tv = textView {
        textSize = 48f
        gravity = gravityCenter
        setTextColor(theme.keyTextColor.color)
    }

    init {
        // TODO: layout items in grid, also backspace button
        add(tv, lParams {
            centerVertically()
            centerHorizontally()
        })
        // Pages must fill the whole ViewPager2 (use match_parent)
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
    }
}