package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ThemeListItemDecoration(val itemWidth: Int, val spanCount: Int) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val offset = (parent.width - itemWidth * spanCount) / (spanCount + 1)
        val halfOffset = offset / 2
        val position = parent.getChildAdapterPosition(view)
        val rowCount = parent.adapter?.run { itemCount / spanCount } ?: -1
        outRect.set(
            if (position % spanCount == 0) offset else halfOffset,
            if (position < spanCount) offset else halfOffset,
            if (position % spanCount == spanCount - 1) offset else halfOffset,
            if (position / spanCount == rowCount) offset else halfOffset
        )
    }
}
