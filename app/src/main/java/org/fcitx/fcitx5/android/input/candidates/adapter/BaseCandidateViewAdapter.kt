package org.fcitx.fcitx5.android.input.candidates.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.applyKeyTextColor

abstract class BaseCandidateViewAdapter :
    RecyclerView.Adapter<BaseCandidateViewAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView as TextView
        var idx = -1
    }

    var candidates: Array<String> = arrayOf()
        private set

    var offset = 0
        private set

    fun getCandidateAt(position: Int) = candidates[offset + position]

    @SuppressLint("NotifyDataSetChanged")
    fun updateCandidates(data: Array<String>) {
        candidates = data
        notifyDataSetChanged()
    }

    fun updateCandidatesWithOffset(data: Array<String>, offset: Int) {
        this.offset = offset
        updateCandidates(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = createTextView(parent)
        return ViewHolder(view).apply {
            theme.applyKeyTextColor(textView)
            itemView.setOnClickListener { onSelect(this.idx + offset) }
            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onTouchDown(v)
                    MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                }
                false
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = getCandidateAt(position)
        holder.idx = position
    }

    abstract val theme: Theme

    override fun getItemCount() = candidates.size - offset

    abstract fun createTextView(parent: ViewGroup): TextView

    abstract fun onTouchDown(view: View)

    abstract fun onSelect(idx: Int)
}