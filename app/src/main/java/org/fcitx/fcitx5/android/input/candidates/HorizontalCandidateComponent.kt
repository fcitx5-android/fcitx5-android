package org.fcitx.fcitx5.android.input.candidates

import android.content.res.Configuration
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.BooleanKey.CandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdated
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxVerticalDecoration
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.core.wrapContent

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val context by manager.context()
    private val builder: CandidateViewBuilder by manager.must()
    private val bar: KawaiiBarComponent by manager.must()

    private val candidateGrowth by AppPrefs.getInstance().keyboard.horizontalCandidateGrowth
    private val maxSpanCount by lazy {
        AppPrefs.getInstance().keyboard.run {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                expandedCandidateGridSpanCount
            else
                expandedCandidateGridSpanCountLandscape
        }.getValue()
    }

    private var layoutViewWidth = 0
    private var layoutViewHeight = 0
    private var layoutFlexGrow = 1f

    /**
     * Second layout pass is needed when total candidates count < [maxSpanCount], but the
     * RecyclerView cannot display all of them. In that case, displayed candidates should be
     * stretched evenly (by setting flexGrow to 1.0f).
     */
    private var secondLayoutPassNeeded = false
    private var secondLayoutPassDone = false

    // Since expanded candidate window is created once the expand button was clicked,
    // we need to replay the last offset
    private val _expandedCandidateOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expandedCandidateOffset = _expandedCandidateOffset.asSharedFlow()

    private fun refreshExpanded() {
        runBlocking {
            _expandedCandidateOffset.emit(view.childCount)
        }
    }

    val adapter: BaseCandidateViewAdapter by lazy {
        builder.flexAdapter {
            val lp = FlexboxLayoutManager.LayoutParams(wrapContent, layoutViewHeight)
            if (candidateGrowth) lp.apply {
                minWidth = layoutViewWidth
                flexGrow = layoutFlexGrow
            } else lp
        }
    }

    val layoutManager by lazy {
        object : FlexboxLayoutManager(context) {
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                if (secondLayoutPassNeeded) {
                    if (childCount >= adapter.candidates.size) {
                        // second layout pass is not actually need,
                        // because RecyclerView can display all the candidates
                        secondLayoutPassNeeded = false
                    } else {
                        // second layout pass would trigger onLayoutCompleted as well,
                        // just skip second onLayoutCompleted
                        if (secondLayoutPassDone) return
                        secondLayoutPassDone = true
                        for (i in 0 until childCount) {
                            getChildAt(i)!!.updateLayoutParams<LayoutParams> {
                                flexGrow = 1f
                            }
                        }
                    }
                }
                refreshExpanded()
                bar.expandButtonStateMachine.push(
                    ExpandedCandidatesUpdated,
                    CandidatesEmpty to (adapter.candidates.size - childCount <= 0)
                )
            }
            // no need to override `generate{,Default}LayoutParams`, because builder.flexAdapter()
            // guarantees ViewHolder's layoutParams to be `FlexboxLayoutManager.LayoutParams`
        }
    }

    private val dividerDrawable by lazy {
        builder.dividerDrawable()
    }

    override val view by lazy {
        object : RecyclerView(context) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                layoutViewWidth = w / maxSpanCount - dividerDrawable.intrinsicWidth
                layoutViewHeight = h
            }
        }.apply {
            id = R.id.candidate_view
            adapter = this@HorizontalCandidateComponent.adapter
            layoutManager = this@HorizontalCandidateComponent.layoutManager
            addItemDecoration(FlexboxVerticalDecoration(dividerDrawable))
        }
    }

    override fun onCandidateUpdate(data: Array<String>) {
        if (candidateGrowth) {
            // second layout pass maybe needed when total candidates count < maxSpanCount
            secondLayoutPassNeeded = data.size < maxSpanCount
            secondLayoutPassDone = false
            layoutFlexGrow = if (secondLayoutPassNeeded) 0f else 1f
        } else {
            secondLayoutPassNeeded = false
        }
        adapter.updateCandidates(data)
    }
}