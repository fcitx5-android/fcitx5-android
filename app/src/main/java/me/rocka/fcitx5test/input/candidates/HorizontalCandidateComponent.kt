package me.rocka.fcitx5test.input.candidates

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.bar.ExpandButtonTransitionEvent.*
import me.rocka.fcitx5test.input.bar.KawaiiBarComponent
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.input.wm.InputWindowManager
import me.rocka.fcitx5test.utils.globalLayoutListener
import me.rocka.fcitx5test.utils.onDataChanged
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
        if (window is ExpandedCandidateWindow)
            bar.expandButtonStateMachine.push(ExpandedCandidatesAttached)
    }

    override fun onWindowDetached(window: InputWindow) {
        if (window is ExpandedCandidateWindow)
            bar.expandButtonStateMachine.push(
                if (isExpandedCandidatesNonEmpty)
                    ExpandedCandidatesDetachedWithNonEmpty
                else
                    ExpandedCandidatesDetachedWithEmpty
            )
    }

    override fun onCandidateUpdates(data: Array<String>) {
        adapter.updateCandidates(data)
    }
}