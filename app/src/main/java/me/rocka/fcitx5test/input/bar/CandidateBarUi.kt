package me.rocka.fcitx5test.input.bar

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.imageResource

class CandidateBarUi(override val ctx: Context, private val horizontalView: View) : Ui {

    val expandButton = imageButton(R.id.expand_candidate_btn) {
        background = null
        imageResource = R.drawable.ic_baseline_expand_more_24
        visibility = ConstraintLayout.INVISIBLE
    }

    override val root = ctx.constraintLayout {
        add(expandButton, lParams(matchConstraints, dp(40)) {
            matchConstraintPercentWidth = 0.1f
            topOfParent()
            endOfParent()
            bottomOfParent()
        })
        add(horizontalView, lParams(matchConstraints, dp(40)) {
            topOfParent()
            startOfParent()
            before(expandButton)
            bottomOfParent()
        })
    }
}
