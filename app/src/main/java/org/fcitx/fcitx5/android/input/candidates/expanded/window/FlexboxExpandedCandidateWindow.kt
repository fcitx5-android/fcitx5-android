/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.input.candidates.adapter.PagingCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxHorizontalDecoration
import splitties.dimensions.dp
import splitties.views.dsl.core.wrapContent
import splitties.views.setPaddingDp

class FlexboxExpandedCandidateWindow :
    BaseExpandedCandidateWindow<FlexboxExpandedCandidateWindow>() {

    override val adapter by lazy {
        object : PagingCandidateViewAdapter(theme) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return super.onCreateViewHolder(parent, viewType).apply {
                    itemView.apply {
                        minimumWidth = dp(40)
                        setPaddingDp(10, 0, 10, 0)
                        layoutParams = FlexboxLayoutManager.LayoutParams(wrapContent, dp(40))
                            .apply { flexGrow = 1f }
                    }
                }
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                holder.itemView.setOnClickListener {
                    fcitx.launchOnReady { it.select(holder.idx) }
                }
            }
        }
    }

    override val layoutManager by lazy {
        FlexboxLayoutManager(context).apply {
            justifyContent = JustifyContent.SPACE_AROUND
            alignItems = AlignItems.FLEX_START
        }
    }

    override fun onCreateCandidateLayout(): ExpandedCandidateLayout =
        ExpandedCandidateLayout(context, theme).apply {
            recyclerView.apply {
                adapter = this@FlexboxExpandedCandidateWindow.adapter
                layoutManager = this@FlexboxExpandedCandidateWindow.layoutManager
                addItemDecoration(FlexboxHorizontalDecoration(dividerDrawable))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        this@FlexboxExpandedCandidateWindow.layoutManager.apply {
                            pageUpBtn.isEnabled = findFirstCompletelyVisibleItemPosition() != 0
                            pageDnBtn.isEnabled =
                                findLastCompletelyVisibleItemPosition() != itemCount - 1
                        }
                    }
                })
            }
        }

    override fun prevPage() {
        layoutManager.apply {
            var prev = findFirstCompletelyVisibleItemPosition() - 1
            if (prev < 0) prev = 0
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_END
                override fun calculateSpeedPerPixel(dm: DisplayMetrics?) =
                    if (disableAnimation) Float.MIN_VALUE else super.calculateSpeedPerPixel(dm)
            }.apply { targetPosition = prev })
        }
    }

    override fun nextPage() {
        layoutManager.apply {
            var next = findLastCompletelyVisibleItemPosition() + 1
            if (next >= itemCount) next = itemCount - 1
            startSmoothScroll(object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference() = SNAP_TO_START
                override fun calculateSpeedPerPixel(dm: DisplayMetrics?) =
                    if (disableAnimation) Float.MIN_VALUE else super.calculateSpeedPerPixel(dm)
            }.apply { targetPosition = next })
        }
    }

}