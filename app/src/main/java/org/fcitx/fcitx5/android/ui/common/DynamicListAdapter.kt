package org.fcitx.fcitx5.android.ui.common

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class DynamicListAdapter<T>(
    initialEntries: List<T>,
    val enableAddAndDelete: Boolean = true,
    val enableOrder: Boolean = false,
    val initCheckBox: (CheckBox.(Int) -> Unit) = { visibility = View.GONE },
    var initEditButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE },
    var initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE }
) :
    RecyclerView.Adapter<DynamicListAdapter<T>.ViewHolder>() {

    private val _entries = initialEntries.toMutableList()

    val entries: List<T>
        // should have use _entries.toList()
        get() = _entries

    private var listener: OnItemChangedListener<T>? = null

    abstract fun showEntry(x: T): String

    fun removeItemChangedListener() {
        listener = null
    }

    fun addOnItemChangedListener(x: OnItemChangedListener<T>) {
        listener = listener?.let { OnItemChangedListener.merge(it, x) } ?: x
    }

    inner class ViewHolder(entryUi: DynamicListEntryUi) : RecyclerView.ViewHolder(entryUi.root) {
        val handleImage = entryUi.handleImage
        val checkBox = entryUi.checkBox
        val nameText = entryUi.nameText
        val editButton = entryUi.editButton
        val settingsButton = entryUi.settingsButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(DynamicListEntryUi(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = entries[position]
        with(holder) {
            handleImage.visibility = if (enableOrder) View.VISIBLE else View.GONE
            nameText.text = showEntry(item)
            initCheckBox(checkBox, position)
            initSettingsButton(settingsButton, position)
            initEditButton(editButton, position)
        }
    }

    override fun getItemCount(): Int = entries.size

    @CallSuper
    open fun addItem(idx: Int = _entries.size, item: T) {
        _entries.add(idx, item)
        notifyItemInserted(idx)
        listener?.onItemAdded(idx, item)
    }

    @CallSuper
    open fun removeItem(idx: Int): T {
        val item = _entries.removeAt(idx)
        notifyItemRemoved(idx)
        listener?.onItemRemoved(idx, item)
        return item
    }

    @CallSuper
    open fun swapItem(fromIdx: Int, toIdx: Int) {
        Collections.swap(_entries, fromIdx, toIdx)
        notifyItemMoved(fromIdx, toIdx)
        listener?.onItemSwapped(fromIdx, toIdx, _entries[toIdx])
    }

    @CallSuper
    open fun updateItem(idx: Int, item: T) {
        val old = _entries[idx]
        _entries[idx] = item
        notifyItemChanged(idx)
        listener?.onItemUpdated(idx, old, item)
    }

    fun indexItem(item: T): Int = _entries.indexOf(item)

}