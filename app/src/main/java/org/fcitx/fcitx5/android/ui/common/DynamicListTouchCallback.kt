/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.color
import splitties.resources.drawable
import splitties.resources.styledColor
import kotlin.math.absoluteValue

open class DynamicListTouchCallback<T>(
    private val ctx: Context,
    private val adapter: DynamicListAdapter<T>
) : ItemTouchHelper.SimpleCallback(
    /* dragDirs = */ if (adapter.enableOrder) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0,
    /* swipeDirs = */ if (adapter.enableAddAndDelete) ItemTouchHelper.LEFT else 0
) {

    private var selected = true
    private var reset = false

    private val deleteBackground: ColorDrawable by lazy {
        ColorDrawable().apply {
            color = ctx.color(R.color.red_400)
        }
    }

    private val deleteIcon: Bitmap by lazy {
        ctx.drawable(R.drawable.ic_baseline_delete_24)!!.apply {
            setTint(ctx.styledColor(android.R.attr.colorBackground))
        }.toBitmap()
    }

    // manually call start drag at the on long click listener
    override fun isLongPressDragEnabled(): Boolean = false

    // disable swipe in multi-selecting and for non-removable items
    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val item = adapter.entries[viewHolder.bindingAdapterPosition]
        if (adapter.multiselect || !adapter.removable(item))
            return if (adapter.enableOrder) ItemTouchHelper.UP or ItemTouchHelper.DOWN
            else ItemTouchHelper.ACTION_STATE_IDLE
        return super.getSwipeDirs(recyclerView, viewHolder)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.swapItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.removeItem(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder?.itemView ?: return super.onChildDrawOver(
            c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
        )
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_SWIPE -> {
                // consider swipe left only, dX < 0
                val canvasLeft = (itemView.right + dX).toInt()
                deleteBackground.apply {
                    bounds = itemView.run { Rect(canvasLeft, top, right, bottom) }
                }.draw(c)
                val iconMargin = (itemView.height - deleteIcon.height) / 2
                val revealed = (dX.absoluteValue - iconMargin).toInt()
                c.drawBitmap(
                    deleteIcon,
                    /* src = */ Rect(
                        /* left = */ if (revealed > deleteIcon.width) 0 else deleteIcon.width - revealed,
                        /* top = */ 0,
                        /* right = */ deleteIcon.width,
                        /* bottom = */ deleteIcon.height
                    ),
                    /* dst = */ Rect(
                        /* left = */ if (revealed > deleteIcon.width) itemView.right - iconMargin - deleteIcon.width else canvasLeft,
                        /* top = */ itemView.top + iconMargin,
                        /* right = */ itemView.right - iconMargin,
                        /* bottom = */ itemView.top + iconMargin + deleteIcon.height
                    ),
                    null
                )
            }
            ItemTouchHelper.ACTION_STATE_DRAG -> {
            }
        }
        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                if (selected) {
                    // elevate only when picked for the first time
                    selected = false
                    viewHolder.itemView.animate().setDuration(200).translationZ(recyclerView.dp(4f))
                }
                if (reset) {
                    // when your item is not floating anymore
                    reset = false
                    selected = true
                    viewHolder.itemView.animate().setDuration(200).translationZ(0f)
                }
            }
            ItemTouchHelper.ACTION_STATE_SWIPE -> {
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // interaction is over, time to reset our elevation
        reset = true
    }
}