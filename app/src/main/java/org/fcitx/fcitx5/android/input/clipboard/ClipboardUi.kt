package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.ViewAnimator
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource
import timber.log.Timber

class ClipboardUi(override val ctx: Context) : Ui {

    val recyclerView = recyclerView {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        addItemDecoration(SpacesItemDecoration(dp(4)))
    }

    val enableUi = ClipboardInstructionUi.Enable(ctx)

    val emptyUi = ClipboardInstructionUi.Empty(ctx)

    override val root = view(::ViewAnimator) {
        add(recyclerView, lParams(matchParent, matchParent))
        add(emptyUi.root, lParams(matchParent, matchParent))
        add(enableUi.root, lParams(matchParent, matchParent))
    }

    val deleteAllButton = imageButton {
        background = styledDrawable(android.R.attr.actionBarItemBackground)
        imageResource = R.drawable.ic_baseline_delete_sweep_24
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        visibility = View.INVISIBLE
    }

    val extension = horizontalLayout {
        add(deleteAllButton, lParams(dp(40), dp(40)))
    }

    private fun setDeleteButtonShown(enabled: Boolean) {
        deleteAllButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    fun switchUiByState(state: ClipboardStateMachine.State) {
        Timber.d("Switch clipboard to $state")
        when (state) {
            ClipboardStateMachine.State.Normal -> {
                root.displayedChild = 0
                setDeleteButtonShown(true)
            }
            ClipboardStateMachine.State.AddMore -> {
                root.displayedChild = 1
                setDeleteButtonShown(false)
            }
            ClipboardStateMachine.State.EnableListening -> {
                root.displayedChild = 2
                setDeleteButtonShown(false)
            }
        }
    }
}