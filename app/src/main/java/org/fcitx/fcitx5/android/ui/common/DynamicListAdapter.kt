package org.fcitx.fcitx5.android.ui.common

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.annotation.CallSuper
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.getHostActivity
import java.util.*

abstract class DynamicListAdapter<T>(
    initialEntries: List<T>,
    val enableAddAndDelete: Boolean = true,
    val enableOrder: Boolean = false,
    val initCheckBox: (CheckBox.(T) -> Unit) = { visibility = View.GONE },
    var initEditButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE },
    var initSettingsButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE }
) :
    RecyclerView.Adapter<DynamicListAdapter<T>.ViewHolder>(), ActionMode.Callback {

    private val _entries = initialEntries.toMutableList()

    val entries: List<T>
        get() = _entries

    var multiselect = false
        private set

    private var actionMode: ActionMode? = null

    // The undo action may change the entries during selection
    // Thus we cannot use position
    private val selected = mutableListOf<T>()

    private var listener: OnItemChangedListener<T>? = null
    protected var itemTouchHelper: ItemTouchHelper? = null

    var removable: (T) -> Boolean = { true }

    abstract fun showEntry(x: T): String
    fun removeItemChangedListener() {
        listener = null
    }

    fun addOnItemChangedListener(x: OnItemChangedListener<T>) {
        listener = listener?.let { OnItemChangedListener.merge(it, x) } ?: x
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            val indexed = selected.mapNotNull { entry ->
                indexItem(entry).takeIf { it != -1 }?.let { it to entry }
            }
            indexed.forEach { (index, _) ->
                _entries.removeAt(index)
                notifyItemRemoved(index)
            }
            listener?.onItemRemovedBatch(indexed)
        }
        mode.finish()
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.dlistm, menu)
        return true
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onDestroyActionMode(mode: ActionMode) {
        multiselect = false
        selected.clear()
        notifyDataSetChanged()
        actionMode = null
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

    inner class ViewHolder(entryUi: DynamicListEntryUi) : RecyclerView.ViewHolder(entryUi.root) {
        val multiselectCheckBox = entryUi.multiselectCheckBox
        val handleImage = entryUi.handleImage
        val checkBox = entryUi.checkBox
        val nameText = entryUi.nameText
        val editButton = entryUi.editButton
        val settingsButton = entryUi.settingsButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(DynamicListEntryUi(parent.context))

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = entries[position]
        with(holder) {
            handleImage.setOnLongClickListener {
                if (!multiselect && removable(item))
                    itemTouchHelper
                        ?.startDrag(this)
                        ?.run { true }
                        ?: false
                else false
            }
            nameText.text = showEntry(item)

            multiselectCheckBox.isChecked = item in selected

            if (multiselect) {
                handleImage.visibility = View.GONE
                multiselectCheckBox.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                settingsButton.visibility = View.GONE
                editButton.visibility = View.GONE
            } else {
                handleImage.visibility = if (enableOrder) View.VISIBLE else View.GONE
                multiselectCheckBox.visibility = View.GONE
                initCheckBox(checkBox, item)
                initSettingsButton(settingsButton, item)
                initEditButton(editButton, item)
            }

            if (enableAddAndDelete && removable(item)) {
                nameText.setOnLongClickListener {
                    if (!multiselect) {
                        multiselect = true
                        select(item, multiselectCheckBox)
                        actionMode = itemView.context.getHostActivity()!!
                            .startSupportActionMode(this@DynamicListAdapter)
                        notifyDataSetChanged()
                    }
                    true
                }
                nameText.setOnClickListener {
                    select(item, multiselectCheckBox)
                }
            }
        }
    }

    private fun select(item: T, checkBox: CheckBox) {
        if (!enableAddAndDelete || !multiselect)
            return
        if (item in selected) {
            selected.remove(item)
            checkBox.isChecked = false
        } else {
            selected.add(item)
            checkBox.isChecked = true
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

    fun exitMultiselect() {
        actionMode?.finish()
    }
}