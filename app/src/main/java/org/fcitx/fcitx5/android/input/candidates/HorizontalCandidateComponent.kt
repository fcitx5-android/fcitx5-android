package org.fcitx.fcitx5.android.input.candidates

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.expanded.window.BaseExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.globalLayoutListener
import org.fcitx.fcitx5.android.utils.onDataChanged
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.recyclerview.recyclerView

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val bar: KawaiiBarComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()

    private var needsRefreshExpanded = false
    private var isExpandedCandidatesNonEmpty = false

    val adapter by lazy {
        builder.simpleAdapter().apply {
            onDataChanged {
                needsRefreshExpanded = true
            }
        }
    }

    // Since expanded candidate window is created once the expand button was clicked,
    // we need to replay the last offset
    private val _expandedCandidateOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expandedCandidateOffset = _expandedCandidateOffset.asSharedFlow()

    private fun refreshExpanded() {
        val candidates = this@HorizontalCandidateComponent.adapter.candidates
        val expandedWillBeNonEmpty = candidates.size - view.childCount > 0
        isExpandedCandidatesNonEmpty = expandedWillBeNonEmpty

        if (candidates.isEmpty())
            windowManager.switchToKeyboardWindow()

        runBlocking {
            _expandedCandidateOffset.emit(view.childCount)
        }
    }

    override val view by lazy {
        context.recyclerView(R.id.candidate_view) {
            isVerticalScrollBarEnabled = false
            with(builder) {
                setupFlexboxLayoutManager(this@HorizontalCandidateComponent.adapter, false)
                addFlexboxVerticalDecoration()
            }
            globalLayoutListener {
                if (needsRefreshExpanded) {
                    refreshExpanded()
                    bar.expandButtonStateMachine.push(
                        if (isExpandedCandidatesNonEmpty)
                            ExpandedCandidatesUpdatedNonEmpty
                        else
                            ExpandedCandidatesUpdatedEmpty
                    )
                    needsRefreshExpanded = false
                }
            }
        }
    }

    override fun onWindowAttached(window: InputWindow) {
        if (window is BaseExpandedCandidateWindow<*>)
            bar.expandButtonStateMachine.push(ExpandedCandidatesAttached)
    }

    override fun onWindowDetached(window: InputWindow) {
        if (window is BaseExpandedCandidateWindow<*>)
            bar.expandButtonStateMachine.push(
                if (isExpandedCandidatesNonEmpty)
                    ExpandedCandidatesDetachedWithCandidatesNonEmpty
                else
                    ExpandedCandidatesDetachedWithCandidatesEmpty
            )
    }

    override fun onCandidateUpdate(data: Array<String>) {
        adapter.updateCandidates(data)
    }
}