package me.rocka.fcitx5test.input.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.input.keyboard.BackspaceKey
import me.rocka.fcitx5test.input.keyboard.BaseKeyboard
import me.rocka.fcitx5test.input.keyboard.ReturnKey
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
class ExpandableCandidateLayout(
    context: Context,
    initRecyclerView: RecyclerView.() -> Unit = {}
) : BaseKeyboard(context, emptyList()) {

    val recyclerView = recyclerView {
        isVerticalScrollBarEnabled = false
    }

    private val pageUpBtn = imageButton {
        elevation = dp(2f)
        // TODO: page
        background = null
        imageResource = R.drawable.ic_baseline_arrow_upward_24
    }

    private val pageDnBtn = imageButton {
        elevation = dp(2f)
        // TODO: page
        background = null
        imageResource = R.drawable.ic_baseline_arrow_downward_24
    }

    private val backspaceBtn = createButton(BackspaceKey())

    private val returnBtn: ImageButton = createButton(ReturnKey()) as ImageButton

    init {
        id = R.id.expanded_candidate_view
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

    override fun onPreeditChange(info: EditorInfo?, content: PreeditContent) {
        updateReturnButton(returnBtn, info, content)
    }
}