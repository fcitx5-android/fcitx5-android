package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import splitties.dimensions.dp

class ResponsiveThemeListView(context: Context) : RecyclerView(context) {

    var itemWidth = dp(128)
    var itemHeight = dp(92)
    var minMargin = dp(4)

    private lateinit var grid: GridLayoutManager

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val spanCount = estimateSpanCount(w)
        if (!this::grid.isInitialized) {
            grid = object : GridLayoutManager(context, spanCount) {
                override fun generateDefaultLayoutParams() = LayoutParams(itemWidth, itemHeight)
            }
            layoutManager = grid
            addItemDecoration(ThemeListItemDecoration(itemWidth, spanCount))
        } else {
            grid.spanCount = spanCount
            invalidateItemDecorations()
        }
    }

    private fun estimateSpanCount(width: Int) = (width - minMargin) / (itemWidth + minMargin)
}
