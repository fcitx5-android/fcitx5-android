package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R

class CandidateViewAdapter(val onSelect: (Int) -> Unit) :
    RecyclerView.Adapter<CandidateViewAdapter.CandidateItemHolder>() {
    class CandidateItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.candidate_item_text)
        var idx = -1
    }

    var candidates: Array<String> = arrayOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item, parent, false)
        return CandidateItemHolder(v).apply {
            itemView.setOnClickListener { onSelect(this.idx) }
        }
    }

    override fun onBindViewHolder(holder: CandidateItemHolder, position: Int) {
        holder.textView.text = candidates[position]
        holder.idx = position
    }

    override fun getItemCount() = candidates.size
}
