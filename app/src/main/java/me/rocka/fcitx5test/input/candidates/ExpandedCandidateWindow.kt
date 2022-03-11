package me.rocka.fcitx5test.input.candidates

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.candidates.adapter.BaseCandidateViewAdapter
import me.rocka.fcitx5test.input.candidates.adapter.GridCandidateViewAdapter
import me.rocka.fcitx5test.input.candidates.adapter.SimpleCandidateViewAdapter
import me.rocka.fcitx5test.input.keyboard.CommonKeyActionListener
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.input.wm.InputWindow
import org.mechdancer.dependency.manager.must


class ExpandedCandidateWindow :
    InputWindow.SimpleInputWindow<ExpandedCandidateWindow>(),
    InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()

    val style: Style by Prefs.getInstance().expandableCandidateStyle

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
                            autoSpanCount()
                            setupGridLayoutManager(
                                this@ExpandedCandidateWindow.adapter as GridCandidateViewAdapter,
                                true
                            )
                            addGridDecoration()
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

