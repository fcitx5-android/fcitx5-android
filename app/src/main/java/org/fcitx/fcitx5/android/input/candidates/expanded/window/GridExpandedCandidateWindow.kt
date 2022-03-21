package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.content.res.Configuration
import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout

class GridExpandedCandidateWindow :
    BaseExpandedCandidateWindow<GridExpandedCandidateWindow>() {

    private val gridSpanCountListener: Prefs.OnChangeListener<Int> by lazy {
        Prefs.OnChangeListener {
            (view.recyclerView.layoutManager as GridLayoutManager).spanCount = value
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


    override val view by lazy {
        ExpandedCandidateLayout(context).apply {
            recyclerView.apply {
                with(builder) {
                    setupGridLayoutManager(
                        this@GridExpandedCandidateWindow.adapter,
                        true
                    )
                    addGridDecoration()
                    (layoutManager as GridLayoutManager).spanCount = gridSpanCountPref.value
                }
            }
        }
    }

    override fun onCandidateUpdates(data: Array<String>) {
        view.resetPosition()
    }

}