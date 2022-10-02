package org.fcitx.fcitx5.android.input.candidates.expanded.decoration

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import splitties.dimensions.dp

class GridDecoration(val drawable: Drawable) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val lp = view.layoutParams as GridLayoutManager.LayoutParams
        val layoutManager = parent.layoutManager as GridLayoutManager
        // add space for items except last in each row
        if (lp.spanIndex + lp.spanSize != layoutManager.spanCount) {
            outRect.right = drawable.intrinsicWidth
        } else {
            outRect.set(0, 0, 0, 0)
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as GridLayoutManager
        for (i in 0 until layoutManager.childCount) {
            val view = parent.getChildAt(i)
            val lp = view.layoutParams as GridLayoutManager.LayoutParams
            // break if it is the last item in data set
            if (i == (parent.adapter!!.itemCount - 1))
                break
            // skip if it is the last item in each row
            if (lp.spanIndex + lp.spanSize == layoutManager.spanCount)
                continue
            val left = view.right + lp.rightMargin
            val right = left + drawable.intrinsicWidth
            val top = view.top - lp.topMargin
            val bottom = view.bottom + lp.bottomMargin
            // make the divider shorter
            val vInset = parent.dp(8)
            drawable.setBounds(left, top + vInset, right, bottom - vInset)
            drawable.draw(c)
        }
    }
}