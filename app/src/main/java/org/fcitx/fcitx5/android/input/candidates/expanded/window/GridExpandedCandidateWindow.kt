package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.content.res.Configuration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout

class GridExpandedCandidateWindow :
    BaseExpandedCandidateWindow<GridExpandedCandidateWindow>() {

    private val gridSpanCountListener: Prefs.OnChangeListener<Int> by lazy {
        Prefs.OnChangeListener {
            layoutManager.spanCount = value
        }
    }

    private val gridSpanCountPref by lazy {
        (if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            Prefs.getInstance().expandedCandidateGridSpanCountPortrait
        else
            Prefs.getInstance().expandedCandidateGridSpanCountLandscape)
            .also { it.registerOnChangeListener(gridSpanCountListener) }
    }

    override val adapter by lazy {
        builder.gridAdapter()
    }

    val layoutManager: GridLayoutManager
        get() = view.recyclerView.layoutManager as GridLayoutManager

    override val view by lazy {
        ExpandedCandidateLayout(context).apply {
            recyclerView.apply {
                with(builder) {
                    setupGridLayoutManager(this@GridExpandedCandidateWindow.adapter, true)
                    addGridDecoration()
                    (layoutManager as GridLayoutManager).spanCount = gridSpanCountPref.value
                }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        this@GridExpandedCandidateWindow.layoutManager.apply {
                            pageUpBtn.isEnabled = findFirstCompletelyVisibleItemPosition() != 0
                            pageDnBtn.isEnabled = findLastCompletelyVisibleItemPosition() != itemCount - 1
                        }
                    }
                })
            }
        }
    }

    override fun onCandidateUpdate(data: Array<String>) {
        view.resetPosition()
    }

    override fun prevPage() {
        layoutManager.apply {
            var prev = findFirstCompletelyVisibleItemPosition() - 1
            if (prev < 0) prev = 0
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_END
            }.apply { targetPosition = prev })
        }
    }

    override fun nextPage() {
        layoutManager.apply {
            var next = findLastCompletelyVisibleItemPosition() + 1
            if (next >= itemCount) next = itemCount - 1
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_START
            }.apply { targetPosition = next })
        }
    }

}