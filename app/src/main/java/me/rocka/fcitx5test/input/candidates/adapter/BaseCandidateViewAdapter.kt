package me.rocka.fcitx5test.input.candidates.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

abstract class BaseCandidateViewAdapter :
    RecyclerView.Adapter<BaseCandidateViewAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView as TextView
        var idx = -1
    }

    var candidates: Array<String> = arrayOf()
        private set

    private var offset = 0

    fun getCandidateAt(position: Int) = candidates[position]

    @SuppressLint("NotifyDataSetChanged")
    fun updateCandidates(data: Array<String>) {
        candidates = data
        notifyDataSetChanged()
    }

    fun updateCandidatesWithOffset(data: Array<String>, offset: Int) {
        this.offset = offset
        updateCandidates(data.sliceArray(offset until data.size))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = createTextView(parent)
        return ViewHolder(view).apply {
            itemView.setOnClickListener { onSelect(this.idx + offset) }
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
        holder.textView.text = getCandidateAt(position)
        holder.idx = position
    }

    override fun getItemCount() = candidates.size

    abstract fun createTextView(parent: ViewGroup): TextView

    abstract fun onTouchDown()

    abstract fun onSelect(idx: Int)
}