package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxHorizontalDecoration

class FlexboxExpandedCandidateWindow :
    BaseExpandedCandidateWindow<FlexboxExpandedCandidateWindow>() {

    override val adapter by lazy {
        builder.simpleAdapter()
    }

    val layoutManager: FlexboxLayoutManager
        get() = candidateLayout.recyclerView.layoutManager as FlexboxLayoutManager

    override fun onCreateCandidateLayout(): ExpandedCandidateLayout =
        ExpandedCandidateLayout(context, theme).apply {
            recyclerView.apply {
                layoutManager = object : FlexboxLayoutManager(context) {
                    init {
                        justifyContent = JustifyContent.SPACE_AROUND
                        alignItems = AlignItems.FLEX_START
                    }

                    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) =
                        LayoutParams(lp).apply { flexGrow = 1f }
                }
                adapter = this@FlexboxExpandedCandidateWindow.adapter
                addItemDecoration(FlexboxHorizontalDecoration(builder.dividerDrawable()))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        (recyclerView.layoutManager as FlexboxLayoutManager).apply {
                            pageUpBtn.isEnabled = findFirstCompletelyVisibleItemPosition() != 0
                            pageDnBtn.isEnabled =
                                findLastCompletelyVisibleItemPosition() != itemCount - 1
                        }
                    }
                })
            }
        }

    override fun prevPage() {
        layoutManager.apply {
            var prev = findFirstCompletelyVisibleItemPosition() - 1
            if (prev < 0) prev = 0
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_END
                override fun calculateSpeedPerPixel(dm: DisplayMetrics?) =
                    if (disableAnimation) Float.MIN_VALUE else super.calculateSpeedPerPixel(dm)
            }.apply { targetPosition = prev })
        }
    }

    override fun nextPage() {
        layoutManager.apply {
            var next = findLastCompletelyVisibleItemPosition() + 1
            if (next >= itemCount) next = itemCount - 1
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_START
                override fun calculateSpeedPerPixel(dm: DisplayMetrics?) =
                    if (disableAnimation) Float.MIN_VALUE else super.calculateSpeedPerPixel(dm)
            }.apply { targetPosition = next })
        }
    }

}