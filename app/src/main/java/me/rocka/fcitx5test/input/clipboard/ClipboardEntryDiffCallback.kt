package me.rocka.fcitx5test.input.clipboard

import androidx.recyclerview.widget.DiffUtil
import me.rocka.fcitx5test.data.clipboard.db.ClipboardEntry

class ClipboardEntryDiffCallback(
    val old: List<ClipboardEntry>,
    val new: List<ClipboardEntry>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].id == new[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition] == new[newItemPosition]
}