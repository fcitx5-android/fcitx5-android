package org.fcitx.fcitx5.android.input.dialog

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import splitties.resources.dimenPxSize
import splitties.resources.styledDimenPxSize
import kotlin.math.max

class DividerDecoration(val drawable: Drawable, val index: Int) : RecyclerView.ItemDecoration() {

    private val dividerHeight = max(drawable.intrinsicHeight, 1)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != index) {
            outRect.set(0, 0, 0, 0)
        } else {
            outRect.set(0, dividerHeight, 0, 0)
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val l = parent.dimenPxSize(androidx.appcompat.R.dimen.abc_select_dialog_padding_start_material)
        val r = parent.styledDimenPxSize(androidx.appcompat.R.attr.dialogPreferredPadding)
        drawable.apply {
            val view = parent.getChildAt(index)
            setBounds(view.left + l, view.top - dividerHeight, view.right - r, view.top)
            draw(c)
        }
    }
}
