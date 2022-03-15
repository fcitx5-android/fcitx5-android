package org.fcitx.fcitx5.android.input.clipboard

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacesItemDecoration(val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) = outRect.run {
        top = space
        bottom = space
        left = space
        right = space
    }
}