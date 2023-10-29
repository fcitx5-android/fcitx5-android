/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
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
        when (parent.layoutDirection) {
            View.LAYOUT_DIRECTION_LTR -> {
                outRect.right = drawable.intrinsicWidth
            }
            View.LAYOUT_DIRECTION_RTL -> {
                outRect.left = drawable.intrinsicWidth
            }
            else -> {
                // should not reach here
                outRect.set(0, 0, 0, 0)
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as FlexboxLayoutManager
        for (i in 0 until layoutManager.childCount) {
            val view = parent.getChildAt(i)
            val lp = view.layoutParams as FlexboxLayoutManager.LayoutParams
            val left: Int
            val right: Int
            when (parent.layoutDirection) {
                View.LAYOUT_DIRECTION_LTR -> {
                    left = view.right + lp.rightMargin
                    right = left + drawable.intrinsicWidth
                }
                View.LAYOUT_DIRECTION_RTL -> {
                    right = view.left + lp.leftMargin
                    left = right - drawable.intrinsicWidth
                }
                else -> {
                    // should not reach here
                    left = view.left
                    right = left + drawable.intrinsicWidth
                }
            }
            val top = view.top - lp.topMargin
            val bottom = view.bottom + lp.bottomMargin
            // make the divider shorter
            val vInset = parent.dp(8)
            drawable.setBounds(left, top + vInset, right, bottom - vInset)
            drawable.draw(c)
        }
    }
}