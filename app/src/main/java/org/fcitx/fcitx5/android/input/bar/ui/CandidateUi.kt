package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.view.View
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.view

class CandidateUi(override val ctx: Context, theme: Theme, private val horizontalView: View) : Ui {

    val expandButton = view(::ToolButton, R.id.expand_candidate_btn) {
        setIcon(R.drawable.ic_baseline_expand_more_24)
        setPressHighlightColor(theme.keyPressHighlightColor)
        visibility = View.INVISIBLE
    }

    override val root = ctx.constraintLayout {
        add(expandButton, lParams(dp(40)) {
            centerVertically()
            endOfParent()
        })
        add(horizontalView, lParams {
            centerVertically()
            startOfParent()
            before(expandButton)
        })
    }
}
