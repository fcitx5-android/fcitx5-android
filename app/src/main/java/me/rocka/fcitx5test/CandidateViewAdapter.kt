package me.rocka.fcitx5test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CandidateViewAdapter : RecyclerView.Adapter<CandidateViewAdapter.CandidateItemHolder>() {
    class CandidateItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.candidate_item_text)
        var idx = -1
    }

    var candidates: List<String> = ArrayList()
    var onSelectCallback: (Int) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item, parent, false)
        val holder = CandidateItemHolder(v)
        holder.textView.setOnClickListener { onSelectCallback(holder.idx) }
        return holder
    }

    override fun onBindViewHolder(holder: CandidateItemHolder, position: Int) {
        holder.textView.text = candidates[position]
        holder.idx = position
    }

    override fun getItemCount(): Int {
        return candidates.size
    }
}
