package org.fcitx.fcitx5.android.input.candidates.expanded.window

import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout

class FlexboxExpandedCandidateWindow :
    BaseExpandedCandidateWindow<FlexboxExpandedCandidateWindow>() {

    override val adapter by lazy {
        builder.simpleAdapter()
    }

    val layoutManager: FlexboxLayoutManager
        get() = view.recyclerView.layoutManager as FlexboxLayoutManager

    override val view by lazy {
        ExpandedCandidateLayout(context).apply {
            recyclerView.apply {
                with(builder) {
                    setupFlexboxLayoutManager(this@FlexboxExpandedCandidateWindow.adapter, true)
                    addFlexboxHorizontalDecoration()
                }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        this@FlexboxExpandedCandidateWindow.layoutManager.apply {
                            pageUpBtn.isEnabled = findFirstCompletelyVisibleItemPosition() != 0
                            pageDnBtn.isEnabled = findLastCompletelyVisibleItemPosition() != itemCount - 1
                        }
                    }
                })
                pageUpBtn.setOnClickListener { prevPage() }
                pageDnBtn.setOnClickListener { nextPage() }
            }
        }
    }

    override fun onCandidateUpdate(data: Array<String>) {
        view.resetPosition()
    }

    fun prevPage() {
        layoutManager.apply {
            var prev = findFirstCompletelyVisibleItemPosition() - 1
            if (prev < 0) prev = 0
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_END
            }.apply { targetPosition = prev })
        }
    }

    fun nextPage() {
        layoutManager.apply {
            var next = findLastCompletelyVisibleItemPosition() + 1
            if (next >= itemCount) next = itemCount - 1
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_START
            }.apply { targetPosition = next })
        }
    }

}