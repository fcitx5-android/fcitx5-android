/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.dialog

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import splitties.resources.styledDimenPxSize
import kotlin.math.max

class SingleDividerDecoration(val drawable: Drawable, val index: Int) : RecyclerView.ItemDecoration() {

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
        val view = parent.findViewHolderForAdapterPosition(index)?.itemView ?: return
        drawable.apply {
            val l = parent.styledDimenPxSize(android.R.attr.listPreferredItemPaddingStart)
            val r = parent.styledDimenPxSize(android.R.attr.listPreferredItemPaddingEnd)
            setBounds(view.left + l, view.top - dividerHeight, view.right - r, view.top)
            draw(c)
        }
    }
}
