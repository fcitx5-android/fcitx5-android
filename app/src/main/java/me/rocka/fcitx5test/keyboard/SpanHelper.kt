package me.rocka.fcitx5test.keyboard

import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.size
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.min

class SpanHelper(
    private val adapter: CandidateViewAdapter,
    private val manager: GridLayoutManager
) : GridLayoutManager.SpanSizeLookup() {

    data class ItemLayout(val spanIndex: Int, val spanSize: Int, val groupIndex: Int)

    private val layout = SparseArray<ItemLayout>()

    private fun getMinSpanSize(position: Int) = min(
        ceil(adapter.measureWidth(position) / 2).toInt(), manager.spanCount
    )

    // clear calculated layout
    private fun invalidate() {
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
     * layout for [position]
     */
    private fun layoutItem(position: Int) {
        // skip calculation if we already have the result
        if (layout.containsKey(position))
            return
        var start = 0
        var span = 0
        var group = 0
        // start from the last known line
        // recalculate from the start of that line
        findFistInLastLine()?.let {
            start = it
            group = layout[it].groupIndex
        }
        for (i in start..position) {
            var size = getMinSpanSize(i)
            val nextSize =
                (i + 1).takeIf { it < adapter.candidates.size }?.let {
                    getMinSpanSize(it)
                }
            if (nextSize != null && manager.spanCount - (span + size) < nextSize) {
                // the rest span space can't hold next item
                // stretch this item to fill the line
                size = manager.spanCount - span
            }
            layout[i] = ItemLayout(span, size, group)
            span += size
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