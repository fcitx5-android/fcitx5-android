package org.fcitx.fcitx5.android.input.candidates.expanded.window

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must

abstract class BaseExpandedCandidateWindow<T : BaseExpandedCandidateWindow<T>> : InputWindow.SimpleInputWindow<T>(),
    InputBroadcastReceiver {

    protected val builder: CandidateViewBuilder by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()

    private val lifecycleCoroutineScope by lazy {
        view.findViewTreeLifecycleOwner()!!.lifecycleScope
    }

    // TODO: Refactor, this shouldn't depend on BaseKeyboard
    abstract override val view: BaseKeyboard

    abstract val adapter: BaseCandidateViewAdapter

    private var offsetJob: Job? = null


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
//        view.resetPosition()
    }
}