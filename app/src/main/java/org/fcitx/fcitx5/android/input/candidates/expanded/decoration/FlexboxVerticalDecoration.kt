package org.fcitx.fcitx5.android.input.candidates.expanded.decoration

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import splitties.dimensions.dp

class FlexboxVerticalDecoration(val drawable: Drawable) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.right = drawable.intrinsicWidth
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as FlexboxLayoutManager
        for (i in 0 until layoutManager.childCount) {
            val view = parent.getChildAt(i)
            val lp = view.layoutParams as FlexboxLayoutManager.LayoutParams
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