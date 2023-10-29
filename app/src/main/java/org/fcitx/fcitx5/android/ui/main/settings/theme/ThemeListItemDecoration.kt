/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil

class ThemeListItemDecoration(val itemWidth: Int, val spanCount: Int) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val columnWidth = parent.width / spanCount
        val offset = (parent.width - itemWidth * spanCount) / (spanCount + 1)
        val halfOffset = offset / 2
        val position = parent.getChildAdapterPosition(view)
        val rowCount = parent.adapter?.run { ceil(itemCount / spanCount.toFloat()).toInt() } ?: -1
        val n = position % spanCount

        when (parent.layoutDirection) {
            View.LAYOUT_DIRECTION_LTR -> {
                outRect.set(
                    (n + 1) * offset + n * (itemWidth - columnWidth),
                    if (position < spanCount) offset else halfOffset,
                    0, // (n + 1) * (columnWidth - itemWidth - offset)
                    if (position / spanCount == rowCount - 1) offset else halfOffset
                )
            }
            View.LAYOUT_DIRECTION_RTL -> {
                outRect.set(
                    0,
                    if (position < spanCount) offset else halfOffset,
                    (n + 1) * offset + n * (itemWidth - columnWidth),
                    if (position / spanCount == rowCount - 1) offset else halfOffset
                )
            }
        }
    }
}
