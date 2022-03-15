package org.fcitx.fcitx5.android.input.candidates.adapter

import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.collection.lruCache
import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.dimen
import splitties.resources.dimenPxSize
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.gravityCenter

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

    override fun createTextView(parent: ViewGroup): TextView =
        parent.context.textView {
            layoutParams = GridLayoutManager.LayoutParams(matchParent, dp(40))
            gravity = gravityCenter
            setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen(R.dimen.candidate_font_size))
            minWidth = dimenPxSize(R.dimen.candidate_min_width)
            isSingleLine = true
        }

}