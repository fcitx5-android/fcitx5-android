/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.BooleanKey.ExpandedCandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesAttached
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesDetached
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.expanded.CandidatesPagingSource
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.candidates.expanded.PagingCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import kotlin.math.max

abstract class BaseExpandedCandidateWindow<T : BaseExpandedCandidateWindow<T>> :
    InputWindow.SimpleInputWindow<T>(), InputBroadcastReceiver {

    protected val service by manager.inputMethodService()
    protected val theme by manager.theme()
    protected val fcitx by manager.fcitx()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val bar: KawaiiBarComponent by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()
    private val returnKeyDrawable: ReturnKeyDrawableComponent by manager.must()

    protected val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private lateinit var candidateLayout: ExpandedCandidateLayout

    protected val dividerDrawable by lazy {
        ShapeDrawable(RectShape()).apply {
            val intrinsicSize = max(1, context.dp(1))
            intrinsicWidth = intrinsicSize
            intrinsicHeight = intrinsicSize
            paint.color = theme.dividerColor
        }
    }

    abstract fun onCreateCandidateLayout(): ExpandedCandidateLayout

    final override fun onCreateView(): View {
        candidateLayout = onCreateCandidateLayout().apply {
            recyclerView.apply {
                // disable item cross-fade animation
                itemAnimator = null
            }
        }
        return candidateLayout
    }

    private val keyActionListener = KeyActionListener { it, source ->
        if (it is KeyAction.LayoutSwitchAction) {
            when (it.act) {
                ExpandedCandidateLayout.Keyboard.UpBtnLabel -> prevPage()
                ExpandedCandidateLayout.Keyboard.DownBtnLabel -> nextPage()
            }
        } else {
            commonKeyActionListener.listener.onKeyAction(it, source)
        }
    }

    abstract val adapter: PagingCandidateViewAdapter
    abstract val layoutManager: RecyclerView.LayoutManager

    private var offsetJob: Job? = null

    private val candidatesPager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = 48,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                CandidatesPagingSource(
                    fcitx,
                    total = horizontalCandidate.adapter.total,
                    offset = adapter.offset
                )
            }
        )
    }
    private var candidatesSubmitJob: Job? = null

    abstract fun prevPage()

    abstract fun nextPage()

    override fun onAttached() {
        bar.expandButtonStateMachine.push(ExpandedCandidatesAttached)
        candidateLayout.embeddedKeyboard.also {
            it.onReturnDrawableUpdate(returnKeyDrawable.resourceId)
            it.keyActionListener = keyActionListener
        }
        offsetJob = service.lifecycleScope.launch {
            horizontalCandidate.expandedCandidateOffset.collect {
                if (it <= 0) {
                    windowManager.attachWindow(KeyboardWindow)
                } else {
                    candidateLayout.resetPosition()
                    adapter.refreshWithOffset(it)
                }
            }
        }
        candidatesSubmitJob = service.lifecycleScope.launch {
            candidatesPager.flow.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    fun bindCandidateUiViewHolder(holder: CandidateViewHolder) {
        holder.itemView.setOnClickListener {
            fcitx.launchOnReady { it.select(holder.idx) }
        }
        holder.itemView.setOnLongClickListener {
            horizontalCandidate.showCandidateActionMenu(holder.idx, holder.text, holder.ui)
            true
        }
    }

    override fun onDetached() {
        bar.expandButtonStateMachine.push(
            ExpandedCandidatesDetached,
            ExpandedCandidatesEmpty to (horizontalCandidate.adapter.total == adapter.offset)
        )
        candidatesSubmitJob?.cancel()
        offsetJob?.cancel()
        candidateLayout.embeddedKeyboard.keyActionListener = null
    }

    override fun onPreeditEmptyStateUpdate(empty: Boolean) {
        if (empty) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

}