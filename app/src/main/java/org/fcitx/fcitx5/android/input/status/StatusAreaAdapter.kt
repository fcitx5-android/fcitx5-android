package org.fcitx.fcitx5.android.input.status

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class StatusAreaAdapter : RecyclerView.Adapter<StatusAreaAdapter.Holder>() {
    inner class Holder(val ui: StatusAreaEntryUi) : RecyclerView.ViewHolder(ui.root)

    var entries: Array<StatusAreaEntry> = arrayOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(StatusAreaEntryUi(parent.context))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        entries[position].let {
            holder.ui.setEntry(it)
            holder.ui.root.setOnClickListener { _ ->
                onItemClick(it)
            }
        }
    }

    override fun getItemCount() = entries.size

    abstract fun onItemClick(it: StatusAreaEntry)
}