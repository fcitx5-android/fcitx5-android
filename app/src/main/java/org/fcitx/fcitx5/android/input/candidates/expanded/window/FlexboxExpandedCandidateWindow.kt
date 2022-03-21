package org.fcitx.fcitx5.android.input.candidates.expanded.window

import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout

class FlexboxExpandedCandidateWindow :
    BaseExpandedCandidateWindow<FlexboxExpandedCandidateWindow>() {

    override val adapter by lazy {
        builder.simpleAdapter()
    }

    override val view by lazy {
        ExpandedCandidateLayout(context).apply {
            recyclerView.apply {
                with(builder) {
                    setupFlexboxLayoutManager(
                        this@FlexboxExpandedCandidateWindow.adapter,
                        true
                    )
                    addFlexboxHorizontalDecoration()
                }
            }
        }
    }

    override fun onCandidateUpdates(data: Array<String>) {
        view.resetPosition()
    }

}