package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.candidates.expanded.SpanHelper
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.GridDecoration

class GridExpandedCandidateWindow :
    BaseExpandedCandidateWindow<GridExpandedCandidateWindow>() {

    private val gridSpanCount by lazy {
        AppPrefs.getInstance().keyboard.run {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                expandedCandidateGridSpanCount
            else
                expandedCandidateGridSpanCountLandscape
        }.getValue()
    }

    override val adapter by lazy {
        builder.gridAdapter()
    }

    val layoutManager: GridLayoutManager
        get() = candidateLayout.recyclerView.layoutManager as GridLayoutManager

    override fun onCreateCandidateLayout(): ExpandedCandidateLayout =
        ExpandedCandidateLayout(context, theme).apply {
            recyclerView.apply {
                layoutManager = GridLayoutManager(context, gridSpanCount).apply {
                    spanSizeLookup = SpanHelper(this@GridExpandedCandidateWindow.adapter, this)
                }
                adapter = this@GridExpandedCandidateWindow.adapter
                addItemDecoration(GridDecoration(builder.dividerDrawable()))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        (recyclerView.layoutManager as GridLayoutManager).apply {
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