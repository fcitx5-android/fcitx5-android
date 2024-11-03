/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.floating

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FcitxEvent.PagedCandidateEvent.LayoutHint
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.recyclerview.recyclerView

class PagedCandidatesUi(override val ctx: Context, val theme: Theme) : Ui {

    private var data = FcitxEvent.PagedCandidateEvent.Data.Empty

    sealed class UiHolder(open val ui: Ui) : RecyclerView.ViewHolder(ui.root) {
        class Candidate(override val ui: LabeledCandidateItemUi) : UiHolder(ui)
        class Pagination(override val ui: PaginationUi) : UiHolder(ui)
    }

    private val candidatesAdapter = object : RecyclerView.Adapter<UiHolder>() {
        override fun getItemCount() =
            data.candidates.size + (if (data.hasPrev || data.hasNext) 1 else 0)

        override fun getItemViewType(position: Int) = if (position < data.candidates.size) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UiHolder {
            return when (viewType) {
                0 -> UiHolder.Candidate(LabeledCandidateItemUi(ctx, theme))
                else -> UiHolder.Pagination(PaginationUi(ctx, theme)).apply {
                    val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
                    ui.root.layoutParams = FlexboxLayoutManager.LayoutParams(wrap, wrap).apply {
                        flexGrow = 1f
                        alignSelf = AlignItems.STRETCH
                        minHeight = ctx.dp(20)
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: UiHolder, position: Int) {
            when (holder) {
                is UiHolder.Candidate -> {
                    val candidate = data.candidates[position]
                    holder.ui.update(candidate, active = position == data.cursorIndex)
                }
                is UiHolder.Pagination -> {
                    holder.ui.update(data)
                }
            }
        }
    }

    private val candidatesLayoutManager = FlexboxLayoutManager(ctx).apply {
        flexWrap = FlexWrap.WRAP
    }

    override val root = recyclerView {
        isFocusable = false
        adapter = candidatesAdapter
        layoutManager = candidatesLayoutManager
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(
        data: FcitxEvent.PagedCandidateEvent.Data,
        orientation: FloatingCandidatesOrientation
    ) {
        this.data = data
        val isVertical = when (orientation) {
            FloatingCandidatesOrientation.Automatic -> data.layoutHint == LayoutHint.Vertical
            FloatingCandidatesOrientation.Horizontal -> false
            FloatingCandidatesOrientation.Vertical -> true
        }
        candidatesLayoutManager.apply {
            if (isVertical) {
                flexDirection = FlexDirection.COLUMN
                alignItems = AlignItems.STRETCH
            } else {
                flexDirection = FlexDirection.ROW
                alignItems = AlignItems.BASELINE
            }
        }
        candidatesAdapter.notifyDataSetChanged()
    }
}
