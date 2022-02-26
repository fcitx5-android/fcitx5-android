package me.rocka.fcitx5test.keyboard.candidates

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource

class ExpandedCandidateUi(
    override val ctx: Context,
    initRecyclerView: RecyclerView.() -> Unit = {}
) : Ui {

    private val recyclerView = recyclerView {
        isVerticalScrollBarEnabled = false
    }

    private val pageUpBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_arrow_upward_24
    }

    private val pageDnBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_arrow_downward_24
    }

    private val backspaceBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_backspace_24
    }

    private val returnBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_keyboard_return_24
        backgroundTintList = styledColorSL(android.R.attr.colorAccent)
        colorFilter = PorterDuffColorFilter(
            styledColor(android.R.attr.colorForegroundInverse), PorterDuff.Mode.SRC_IN
        )
    }

    override val root = constraintLayout(R.id.expanded_candidate_view) {
        backgroundColor = styledColor(android.R.attr.colorBackground)

        add(pageUpBtn, lParams(matchConstraints, dp(60)) {
            matchConstraintPercentWidth = 0.15f
            topOfParent()
            endOfParent()
            above(pageDnBtn)
        })

        add(pageDnBtn, lParams(matchConstraints, dp(60)) {
            matchConstraintPercentWidth = 0.15f
            below(pageUpBtn)
            endOfParent()
            above(backspaceBtn)
        })

        add(backspaceBtn, lParams(matchConstraints, dp(60)) {
            matchConstraintPercentWidth = 0.15f
            below(pageDnBtn)
            endOfParent()
            above(returnBtn)
        })

        add(returnBtn, lParams(matchConstraints, dp(60)) {
            matchConstraintPercentWidth = 0.15f
            below(backspaceBtn)
            endOfParent()
            bottomOfParent()
        })

        add(recyclerView, lParams {
            topOfParent()
            startOfParent()
            before(pageUpBtn)
            bottomOfParent()
        })

        initRecyclerView(recyclerView)
    }

    fun resetPosition() {
        recyclerView.scrollToPosition(0)
    }
}