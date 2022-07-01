package org.fcitx.fcitx5.android.input.candidates.expanded

import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class SpanHelper(
    private val adapter: GridCandidateViewAdapter,
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
     * Calculate layout for [position] to [layout]
     */
    // TODO maybe layout items in batch
    // layout manager tend to call getSpanIndex.. methods one by one,
    // causing layoutItem method can only layout 1 item per call
    private fun layoutItem(position: Int) {
        // skip calculation if we already have the result
        if (layoutCount > position)
            return
        var span = 0
        var group = 0
        // start layout from the last known position
        if (layoutCount > 0) {
            val last = layout[layoutCount - 1]!!
            val lastSpan = last.spanIndex + last.spanSize
            if (lastSpan == manager.spanCount) {
                // the last known item is at the end of its group
                // start from beginning of next group
                group = last.groupIndex + 1
            } else {
                // start from last span position
                span = lastSpan
                group = last.groupIndex
            }
        }
        for (i in layoutCount..position) {
            var size = getMinSpanSize(i)
            val nextSize = if (i + 1 < adapter.itemCount) getMinSpanSize(i + 1) else null
            // we still have rest span spaces,
            // but it can't hold the next item
            if (nextSize != null && span + size + nextSize > manager.spanCount) {
                // stretch this item to fill the line
                size = manager.spanCount - span
            }
            // save calculated layout
            layout[i] = ItemLayout(span, size, group)
            layoutCount++
            // accumulate span size
            span += size
            // bump group index
            if (span == manager.spanCount) {
                span = 0
                group++
            }
        }
    }

    override fun getSpanIndex(position: Int, spanCount: Int): Int {
        layoutItem(position)
        return layout[position]!!.spanIndex
    }

    override fun getSpanGroupIndex(adapterPosition: Int, spanCount: Int): Int {
        layoutItem(adapterPosition)
        return layout[adapterPosition]!!.groupIndex
    }

    override fun getSpanSize(position: Int): Int {
        layoutItem(position)
        return layout[position]!!.spanSize
    }
}