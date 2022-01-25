package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import splitties.dimensions.dp
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter
import splitties.views.horizontalPadding

abstract class CandidateViewAdapter :
    RecyclerView.Adapter<CandidateViewAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView as TextView
        var idx = -1
    }

    var candidates: Array<String> = arrayOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.textView {
            layoutParams = ViewGroup.LayoutParams(wrapContent, dp(40))
            gravity = gravityVerticalCenter
            horizontalPadding = dp(10)
            textSize = 20f // sp
        }
        return ViewHolder(view).apply {
            itemView.setOnClickListener { onSelect(this.idx) }
            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onTouchDown()
                    MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                }
                false
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = candidates[position]
        holder.idx = position
    }

    override fun getItemCount() = candidates.size

    abstract fun onTouchDown()

    abstract fun onSelect(idx: Int)
}
