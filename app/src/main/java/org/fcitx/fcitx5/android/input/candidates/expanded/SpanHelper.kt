/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.input.candidates.adapter.GridPagingCandidateViewAdapter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class SpanHelper(
    private val adapter: GridPagingCandidateViewAdapter,
    private val manager: GridLayoutManager
) : GridLayoutManager.SpanSizeLookup() {

    data class ItemLayout(val spanIndex: Int, val spanSize: Int, val groupIndex: Int)

    private val layout = ArrayList<ItemLayout>()

    private fun getMinSpanSize(position: Int) = min(
        // approximately three characters or one Chinese characters per span
        // at least one span for each word, in case measureWidth got zero due to whatever font issues
        max(1, ceil(adapter.measureWidth(position) / 1.5).toInt()), manager.spanCount
    )

    /**
     * clear calculated layout
     */
    private fun invalidate() {
        layout.clear()
    }

    override fun invalidateSpanIndexCache() {
        invalidate()
        super.invalidateSpanIndexCache()
    }

    /**
     * Ensure layout of item at [position] in [adapter] have been calculated and return its layout
     */
    private fun layoutItem(position: Int): ItemLayout {
        // skip calculation if we already have the result
        if (layout.size > position) return layout[position]
        val spanCount = manager.spanCount
        var span = 0
        var group = 0
        // start layout from the last known position
        layout.lastOrNull()?.also { last ->
            val lastSpan = last.spanIndex + last.spanSize
            if (lastSpan == spanCount) {
                // the last known item is at the end of its group
                // start from beginning of next group
                group = last.groupIndex + 1
            } else {
                // start from last span position
                span = lastSpan
                group = last.groupIndex
            }
        }
        val itemCount = adapter.itemCount
        // layout a row of items each time
        val batchEnd = min(spanCount * (position / spanCount + 1), adapter.itemCount)
        for (i in layout.size until batchEnd) {
            var size = getMinSpanSize(i)
            val nextSize = if (i + 1 < itemCount) getMinSpanSize(i + 1) else null
            // we still have rest span spaces,
            // but it can't hold the next item
            if (nextSize != null && span + size + nextSize > spanCount) {
                // stretch this item to fill the line
                size = spanCount - span
            }
            // save calculated layout
            layout.add(ItemLayout(span, size, group))
            // accumulate span size
            span += size
            // bump group index
            if (span == spanCount) {
                span = 0
                group++
            }
        }
        return layout[position]
    }

    override fun getSpanIndex(position: Int, spanCount: Int): Int {
        return layoutItem(position).spanIndex
    }

    override fun getSpanGroupIndex(adapterPosition: Int, spanCount: Int): Int {
        return layoutItem(adapterPosition).groupIndex
    }

    override fun getSpanSize(position: Int): Int {
        return layoutItem(position).spanSize
    }
}