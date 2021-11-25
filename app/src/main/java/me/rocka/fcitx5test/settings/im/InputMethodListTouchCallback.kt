package me.rocka.fcitx5test.settings.im

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.RecyclerView


class InputMethodListTouchCallback(private val adapter: InputMethodListAdapter) :
    ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT
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
            ACTION_STATE_DRAG -> {
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
            ACTION_STATE_SWIPE -> {}
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // interaction is over, time to reset our elevation
        reset = true;
    }
}