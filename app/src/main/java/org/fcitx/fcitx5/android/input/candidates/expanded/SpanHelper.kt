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

    private var layout: Array<ItemLayout?> = arrayOfNulls(adapter.itemCount)

    private var layoutCount = 0

    private fun getMinSpanSize(position: Int) = min(
        // approximately three characters or one Chinese characters per span
        // at least one span for each word, in case measureWidth got zero due to whatever font issues
        max(1, ceil(adapter.measureWidth(position) / 1.5).toInt()), manager.spanCount
    )

    /**
     * clear calculated layout
     */
    private fun invalidate() {
        // reallocate array only when space not enough
        if (adapter.itemCount > layout.size) {
            layout = arrayOfNulls(adapter.itemCount)
        }
        layoutCount = 0
    }

    override fun invalidateSpanIndexCache() {
        invalidate()
        super.invalidateSpanIndexCache()
    }

    /**
     * Calculate layout for all items currently in [adapter]
     */
    private fun layoutItems() {
        val itemCount = adapter.itemCount
        // skip calculation if we already have the result
        if (layoutCount >= itemCount) return
        // Sometimes `GridLayoutManager` won't call `invalidateSpanIndexCache()` when adapter
        // changes occurs for the first time (ie. from itemCount==0 to itemCount!=0)
        // So we need to call `invalidate()` ourselves
        if (layout.size < itemCount) invalidate()
        val spanCount = manager.spanCount
        var span = 0
        var group = 0
        // start layout from the last known position
        if (layoutCount > 0) {
            val last = layout[layoutCount - 1]!!
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
        for (i in layoutCount until itemCount) {
            var size = getMinSpanSize(i)
            val nextSize = if (i + 1 < itemCount) getMinSpanSize(i + 1) else null
            // we still have rest span spaces,
            // but it can't hold the next item
            if (nextSize != null && span + size + nextSize > spanCount) {
                // stretch this item to fill the line
                size = spanCount - span
            }
            // save calculated layout
            layout[i] = ItemLayout(span, size, group)
            layoutCount++
            // accumulate span size
            span += size
            // bump group index
            if (span == spanCount) {
                span = 0
                group++
            }
        }
    }

    override fun getSpanIndex(position: Int, spanCount: Int): Int {
        layoutItems()
        return layout[position]!!.spanIndex
    }

    override fun getSpanGroupIndex(adapterPosition: Int, spanCount: Int): Int {
        layoutItems()
        return layout[adapterPosition]!!.groupIndex
    }

    override fun getSpanSize(position: Int): Int {
        layoutItems()
        return layout[position]!!.spanSize
    }
}