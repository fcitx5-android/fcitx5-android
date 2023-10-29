/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.adapter

import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.core.matchParent

abstract class GridPagingCandidateViewAdapter(theme: Theme) : PagingCandidateViewAdapter(theme) {

    // cache measureWidth
    private val measuredWidths = LruCache<String, Float>(200)

    fun measureWidth(position: Int): Float {
        val candidate = getItem(position) ?: return 0f
        return measuredWidths[candidate] ?: run {
            val paint = Paint()
            val bounds = Rect()
            // 20f here is chosen randomly, since we only care about the ratio
            paint.textSize = 20f
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            (bounds.width() / 20f).also { measuredWidths.put(candidate, it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            itemView.apply {
                layoutParams = GridLayoutManager.LayoutParams(matchParent, dp(40))
            }
        }
    }
}