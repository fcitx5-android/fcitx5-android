package org.fcitx.fcitx5.android.input.candidates

import android.content.res.Configuration
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.SimpleCandidateViewAdapter
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must


class ExpandedCandidateWindow :
    InputWindow.SimpleInputWindow<ExpandedCandidateWindow>(),
    InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()

    val style: Style by Prefs.getInstance().expandableCandidateStyle

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

    private val lifecycleCoroutineScope by lazy {
        view.findViewTreeLifecycleOwner()!!.lifecycleScope
    }

    private var offsetJob: Job? = null

    enum class Style {
        Grid,
        Flexbox;

        companion object : Prefs.StringLikeCodec<Style> {
            override fun decode(raw: String): Style? =
                runCatching { valueOf(raw) }.getOrNull()
        }
    }


    val adapter: BaseCandidateViewAdapter by lazy {
        when (style) {
            Style.Grid -> builder.gridAdapter()
            Style.Flexbox -> builder.simpleAdapter()
        }
    }


    override val view by lazy {
        ExpandedCandidateLayout(context).apply {
            recyclerView.apply {
                with(builder) {
                    when (style) {
                        Style.Grid -> {
                            setupGridLayoutManager(
                                this@ExpandedCandidateWindow.adapter as GridCandidateViewAdapter,
                                true
                            )
                            addGridDecoration()
                            (layoutManager as GridLayoutManager).spanCount = gridSpanCountPref.value
                        }
                        Style.Flexbox -> {
                            setupFlexboxLayoutManager(
                                this@ExpandedCandidateWindow.adapter as SimpleCandidateViewAdapter,
                                true
                            )
                            addFlexboxHorizontalDecoration()
                        }
                    }
                }
            }
        }
    }


    override fun onAttached() {
        view.keyActionListener = commonKeyActionListener.listener
        offsetJob = horizontalCandidate.expandedCandidateOffset.onEach {
            if (it > 0)
                adapter.updateCandidatesWithOffset(horizontalCandidate.adapter.candidates, it)
        }.launchIn(lifecycleCoroutineScope)
    }

    override fun onDetached() {
        offsetJob?.cancel()
        offsetJob = null
        view.keyActionListener = null
    }

    override fun onPreeditUpdate(content: PreeditContent) {
        view.onPreeditChange(null, content)
    }

    override fun onCandidateUpdates(data: Array<String>) {
        view.resetPosition()
    }

}

