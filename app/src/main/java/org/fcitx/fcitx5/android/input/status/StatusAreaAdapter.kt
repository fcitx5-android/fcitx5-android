package org.fcitx.fcitx5.android.input.status

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.core.Action

abstract class StatusAreaAdapter : RecyclerView.Adapter<StatusAreaAdapter.Holder>() {
    inner class Holder(val ui: StatusAreaEntryUi) : RecyclerView.ViewHolder(ui.root)

    var entries: Array<Action> = arrayOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(StatusAreaEntryUi(parent.context))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val action = entries[position]
        holder.ui.setAction(action)
        holder.ui.root.setOnClickListener {
            onItemClick(action.id)
        }
    }

    override fun getItemCount() = entries.size

    abstract fun onItemClick(actionId: Int)
}