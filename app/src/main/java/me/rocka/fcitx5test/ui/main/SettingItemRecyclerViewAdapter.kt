package me.rocka.fcitx5test.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.databinding.SettingEntryBinding

class SettingItemRecyclerViewAdapter(
    private vararg val settingItems: Pair<String, () -> Unit>
) : RecyclerView.Adapter<SettingItemRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            SettingEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (string, action) = settingItems[position]
        holder.run {
            textView.text = string
            rootView.setOnClickListener { action() }
        }
    }

    override fun getItemCount(): Int = settingItems.size

    inner class ViewHolder(binding: SettingEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView = binding.content
        val rootView = binding.root
    }

}