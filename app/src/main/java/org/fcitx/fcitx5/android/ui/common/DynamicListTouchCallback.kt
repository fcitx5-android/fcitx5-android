package org.fcitx.fcitx5.android.ui.common

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.appColor
import splitties.resources.appDrawable
import splitties.resources.appStyledColor
import kotlin.math.absoluteValue

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

    private val deleteBackground by lazy {
        ColorDrawable().apply {
            color = appColor(R.color.red_400)
        }
    }

    private val deleteIcon by lazy {
        appDrawable(R.drawable.ic_baseline_delete_24)!!.toBitmap()
    }

    private val deleteIconPaint by lazy {
        Paint().apply {
            colorFilter = PorterDuffColorFilter(
                appStyledColor(android.R.attr.colorBackground),
                PorterDuff.Mode.SRC_IN
            )
        }
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
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_SWIPE -> {
                // consider swipe left only, dX < 0
                val canvasLeft = (itemView.right + dX).toInt()
                deleteBackground.apply {
                    bounds = itemView.run { Rect(canvasLeft, top, right, bottom) }
                }.draw(c)
                deleteIcon.also {
                    val iconMargin = (itemView.height - it.height) / 2
                    val revealed = (dX.absoluteValue - iconMargin).toInt()
                    c.drawBitmap(
                        it,
                        /* src= */ Rect(
                            /* right = */ if (revealed > it.width) 0 else it.width - revealed,
                            /* top   = */ 0,
                            /* left  = */ it.width,
                            /* bottom= */ it.height
                        ),
                        /* dst= */ Rect(
                            /* right = */ if (revealed > it.width) itemView.right - iconMargin - it.width else canvasLeft,
                            /* top   = */ itemView.top + iconMargin,
                            /* left  = */ itemView.right - iconMargin,
                            /* bottom= */ itemView.top + iconMargin + it.height
                        ),
                        deleteIconPaint
                    )
                }
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