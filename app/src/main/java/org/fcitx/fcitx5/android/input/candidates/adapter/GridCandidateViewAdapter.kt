package org.fcitx.fcitx5.android.input.candidates.adapter

import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewGroup
import androidx.collection.lruCache
import androidx.recyclerview.widget.GridLayoutManager
import splitties.dimensions.dp
import splitties.views.dsl.core.matchParent

abstract class GridCandidateViewAdapter : BaseCandidateViewAdapter() {

    // cache measureWidth
    private val measuredWidths = lruCache<String, Float>(200)

    fun measureWidth(position: Int): Float {
        val candidate = getCandidateAt(position)
        return measuredWidths[candidate] ?: run {
            val paint = Paint()
            val bounds = Rect()
            // 20f here is chosen randomly, since we only care about the ratio
            paint.textSize = 20f
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            (bounds.width() / 20f).also { measuredWidths.put(candidate, it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            ui.root.apply {
                layoutParams = GridLayoutManager.LayoutParams(matchParent, dp(40))
            }
        }
    }
}