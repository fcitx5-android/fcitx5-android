package org.fcitx.fcitx5.android.input.candidates.expanded

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.keyboard.BackspaceKey
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.ImageKeyView
import org.fcitx.fcitx5.android.input.keyboard.ReturnKey
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource

// TODO: Refactor, this shouldn't depend on BaseKeyboard
@SuppressLint("ViewConstructor")
class ExpandedCandidateLayout(
    context: Context,
    initRecyclerView: RecyclerView.() -> Unit = {}
) : BaseKeyboard(context, emptyList()) {

    val recyclerView = recyclerView {
        isVerticalScrollBarEnabled = false
    }

    val pageUpBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_arrow_upward_24
    }

    val pageDnBtn = imageButton {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_arrow_downward_24
    }

    private val backspaceBtn = createKeyView(BackspaceKey()) as ImageKeyView

    private val returnBtn = createKeyView(ReturnKey()) as ImageKeyView

    init {
        id = R.id.expanded_candidate_view
        backgroundColor = styledColor(android.R.attr.colorBackground)

        add(pageUpBtn, lParams(matchConstraints, matchConstraints) {
            matchConstraintPercentWidth = 0.15f
            topOfParent()
            endOfParent()
            above(pageDnBtn)
        })

        add(pageDnBtn, lParams(matchConstraints, matchConstraints) {
            matchConstraintPercentWidth = 0.15f
            below(pageUpBtn)
            endOfParent()
            above(backspaceBtn)
        })

        add(backspaceBtn, lParams(matchConstraints, matchConstraints) {
            matchConstraintPercentWidth = 0.15f
            below(pageDnBtn)
            endOfParent()
            above(returnBtn)
        })

        add(returnBtn, lParams(matchConstraints, matchConstraints) {
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

    override fun onPreeditChange(info: EditorInfo?, content: PreeditContent) {
        updateReturnButton(returnBtn, info, content)
    }
}