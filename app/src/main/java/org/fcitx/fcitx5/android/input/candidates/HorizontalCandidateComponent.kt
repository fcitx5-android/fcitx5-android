package org.fcitx.fcitx5.android.input.candidates

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdatedEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdatedNonEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxVerticalDecoration
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.recyclerview.recyclerView

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val bar: KawaiiBarComponent by manager.must()

    val adapter by lazy {
        builder.simpleAdapter()
    }

    // Since expanded candidate window is created once the expand button was clicked,
    // we need to replay the last offset
    private val _expandedCandidateOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expandedCandidateOffset = _expandedCandidateOffset.asSharedFlow()

    val horizontalCandidateGrowth by AppPrefs.getInstance().keyboard.horizontalCandidateGrowth

    private fun refreshExpanded() {
        runBlocking {
            _expandedCandidateOffset.emit(view.childCount)
        }
    }

    override val view by lazy {
        context.recyclerView(R.id.candidate_view) {
            adapter = this@HorizontalCandidateComponent.adapter
            layoutManager = object : FlexboxLayoutManager(context) {
                override fun canScrollVertically(): Boolean = false
                override fun canScrollHorizontally(): Boolean = false
                override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) =
                    LayoutParams(lp).apply {
                        flexGrow = if (horizontalCandidateGrowth) 1f else 0f
                    }

                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)
                    refreshExpanded()
                    bar.expandButtonStateMachine.push(
                        if (adapter!!.itemCount - childCount > 0)
                            ExpandedCandidatesUpdatedNonEmpty
                        else
                            ExpandedCandidatesUpdatedEmpty
                    )
                }
            }
            addItemDecoration(FlexboxVerticalDecoration(builder.dividerDrawable()))
        }
    }

    override fun onCandidateUpdate(data: Array<String>) {
        adapter.updateCandidates(data)
    }
}