package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

abstract class BaseExpandedCandidateWindow<T : BaseExpandedCandidateWindow<T>> :
    InputWindow.SimpleInputWindow<T>(), InputBroadcastReceiver {

    protected val builder: CandidateViewBuilder by manager.must()
    protected val theme by manager.theme()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val bar: KawaiiBarComponent by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()

    private lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    protected lateinit var candidateLayout: ExpandedCandidateLayout
        private set

    abstract fun onCreateCandidateLayout(): ExpandedCandidateLayout

    final override fun onCreateView(): View {
        candidateLayout = onCreateCandidateLayout()
        return candidateLayout
    }

    private val keyActionListener = BaseKeyboard.KeyActionListener {
        if (it is KeyAction.LayoutSwitchAction) {
            when (it.act) {
                ExpandedCandidateLayout.Keyboard.UpBtnLabel -> prevPage()
                ExpandedCandidateLayout.Keyboard.DownBtnLabel -> nextPage()
            }
        } else {
            commonKeyActionListener.listener.onKeyAction(it)
        }
    }

    abstract val adapter: BaseCandidateViewAdapter

    private var offsetJob: Job? = null

    abstract fun prevPage()

    abstract fun nextPage()

    override fun onAttached() {
        lifecycleCoroutineScope = candidateLayout.findViewTreeLifecycleOwner()!!.lifecycleScope
        bar.expandButtonStateMachine.push(ExpandedCandidatesAttached)
        candidateLayout.embeddedKeyboard.keyActionListener = keyActionListener
        offsetJob = horizontalCandidate.expandedCandidateOffset.onEach {
            adapter.updateCandidatesWithOffset(horizontalCandidate.adapter.candidates, it)
        }.launchIn(lifecycleCoroutineScope)
    }

    override fun onDetached() {
        bar.expandButtonStateMachine.push(
            if (adapter.candidates.size > adapter.offset)
                ExpandedCandidatesDetachedWithCandidatesNonEmpty
            else
                ExpandedCandidatesDetachedWithCandidatesEmpty
        )
        offsetJob?.cancel()
        offsetJob = null
        candidateLayout.embeddedKeyboard.keyActionListener = null
    }

    override fun onCandidateUpdate(data: Array<String>) {
        if (data.isEmpty()) {
            windowManager.switchToKeyboardWindow()
            return
        }
        candidateLayout.resetPosition()
    }

    override fun onPreeditUpdate(content: PreeditContent) {
        if (content.preedit.run { preedit.isEmpty() && clientPreedit.isEmpty() }) {
            windowManager.switchToKeyboardWindow()
        }
    }
}