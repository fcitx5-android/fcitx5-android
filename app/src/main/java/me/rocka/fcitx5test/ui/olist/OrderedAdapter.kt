package me.rocka.fcitx5test.ui.olist

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class OrderedAdapter<T>(
    initialEntries: List<T>,
    val enableOrder: Boolean = true,
    var initEditButton: (Button.(Int) -> Unit) = { visibility = View.GONE },
    var initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE }
) :
    RecyclerView.Adapter<OrderedAdapter<T>.ViewHolder>() {

    private val _entries = initialEntries.toMutableList()

    val entries: List<T>
        // should have use _entries.toList()
        get() = _entries

    private var listener: OnItemChangedListener<T>? = null

    abstract fun showEntry(x: T): String

    fun setOnItemChangedListener(x: OnItemChangedListener<T>?) {
        listener = x
    }

    inner class ViewHolder(entryUi: OrderedListEntryUi) : RecyclerView.ViewHolder(entryUi.root) {
        val handleImage = entryUi.handleImage
        val nameText = entryUi.nameText
        val editButton = entryUi.editButton
        val settingsButton = entryUi.settingsButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(OrderedListEntryUi(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = _entries[position]
        Log.d(javaClass.name, "Binding holder: ${showEntry(item)}")
        with(holder) {
            handleImage.visibility = if (enableOrder) View.VISIBLE else View.GONE
            nameText.text = showEntry(item)
            initSettingsButton(settingsButton, position)
            initEditButton(editButton, position)
        }
    }

    override fun getItemCount(): Int = _entries.size

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

}