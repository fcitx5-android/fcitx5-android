package org.fcitx.fcitx5.android.input.candidates

import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.size
import androidx.core.util.valueIterator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import kotlin.math.ceil
import kotlin.math.min

class SpanHelper(
    private val adapter: GridCandidateViewAdapter,
    private val manager: GridLayoutManager
) : GridLayoutManager.SpanSizeLookup() {

    data class ItemLayout(val spanIndex: Int, val spanSize: Int, val groupIndex: Int)

    private val layout = SparseArray<ItemLayout>()

    val lines: List<List<ItemLayout>>
        get() = layout.valueIterator()
            .asSequence()
            .groupBy { it.groupIndex }
            .map { it.value }

    fun getItemLayout(position: Int): ItemLayout? = layout.get(position)

    private fun getMinSpanSize(position: Int) = min(
        // approximately three characters or one Chinese characters per span
        ceil(adapter.measureWidth(position) / 1.5).toInt(), manager.spanCount
    )

    // clear calculated layout
    fun invalidate() {
        layout.clear()
    }

    private fun findFistInLastLine(): Int? {
        for (i in layout.size - 1 downTo 0) {
            if (layout[i].spanIndex == 0)
                return i
        }
        return null
    }

    /**
     * Calculate layout for [position] to [layout]
     */
    private fun layoutItem(position: Int) {
        // skip calculation if we already have the result
        if (layout.containsKey(position))
            return
        var start = 0
        var span = 0
        var group = 0
        // we start from the last known line
        // recalculate from the start of that line to get a correct span
        findFistInLastLine()?.let {
            start = it
            group = layout[it].groupIndex
        }
        for (i in start..position) {
            var size = getMinSpanSize(i)
            val nextSize =
                (i + 1).takeIf { it < adapter.itemCount }?.let {
                    getMinSpanSize(it)
                }

            // we still have rest span spaces,
            // but it can't hold the next item
            if (nextSize != null && manager.spanCount - (span + size) < nextSize) {
                // stretch this item to fill the line
                size = manager.spanCount - span
            }
            // save calculated layout
            layout.put(i, ItemLayout(span, size, group))
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
        return layout[position].spanIndex
    }

    override fun getSpanGroupIndex(adapterPosition: Int, spanCount: Int): Int {
        layoutItem(adapterPosition)
        return layout[adapterPosition].groupIndex
    }

    override fun getSpanSize(position: Int): Int {
        layoutItem(position)
        return layout[position].spanSize
    }

    fun attach() {
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                invalidate()
            }
        })
        manager.spanSizeLookup = this
    }

}