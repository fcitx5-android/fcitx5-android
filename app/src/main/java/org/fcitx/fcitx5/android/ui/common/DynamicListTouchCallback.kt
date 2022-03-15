package org.fcitx.fcitx5.android.ui.common

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

open class DynamicListTouchCallback<T>(private val adapter: DynamicListAdapter<T>) :
    ItemTouchHelper.SimpleCallback(
        if (adapter.enableOrder)
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        else ItemTouchHelper.ACTION_STATE_IDLE,
        if (adapter.enableAddAndDelete)
            ItemTouchHelper.LEFT
        else ItemTouchHelper.ACTION_STATE_IDLE
    ) {
    private var selected = true
    private var reset = false

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
                    viewHolder.itemView.animate().apply {
                        translationZ(16f)
                        duration = 200
                    }.start()
                }
                if (reset) {
                    // when your item is not floating anymore
                    reset = false
                    selected = true
                    viewHolder.itemView.animate().apply {
                        translationZ(0f)
                        duration = 200
                    }.start()
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