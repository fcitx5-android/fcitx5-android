package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.content.res.Configuration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout

class GridExpandedCandidateWindow :
    BaseExpandedCandidateWindow<GridExpandedCandidateWindow>() {

    private val gridSpanCountListener: ManagedPreference.OnChangeListener<Int> by lazy {
        ManagedPreference.OnChangeListener {
            layoutManager.spanCount = getValue()
        }
    }

    private val gridSpanCountPref by lazy {
        (if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            AppPrefs.getInstance().keyboard.expandedCandidateGridSpanCountPortrait
        else
            AppPrefs.getInstance().keyboard.expandedCandidateGridSpanCountLandscape)
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
                    (layoutManager as GridLayoutManager).spanCount = gridSpanCountPref.getValue()
                }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        this@GridExpandedCandidateWindow.layoutManager.apply {
                            pageUpBtn.isEnabled = findFirstCompletelyVisibleItemPosition() != 0
                            pageDnBtn.isEnabled =
                                findLastCompletelyVisibleItemPosition() != itemCount - 1
                        }
                    }
                })
            }
        }
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