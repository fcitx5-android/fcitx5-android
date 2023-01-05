package org.fcitx.fcitx5.android.ui.common

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import java.util.*

abstract class DynamicListAdapter<T>(
    initialEntries: List<T>,
    val enableAddAndDelete: Boolean = true,
    val enableOrder: Boolean = false,
    val initCheckBox: (CheckBox.(T) -> Unit) = { visibility = View.GONE },
    var initEditButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE },
    var initSettingsButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE }
) :
    RecyclerView.Adapter<DynamicListAdapter<T>.ViewHolder>() {

    private val _entries = initialEntries.toMutableList()

    val entries: List<T>
        get() = _entries

    var multiselect = false
        private set

    private val selected = mutableListOf<T>()

    private var listener: OnItemChangedListener<T>? = null
    protected var itemTouchHelper: ItemTouchHelper? = null

    var removable: (T) -> Boolean = { true }
    private var onBackPressedCallback: OnBackPressedCallback? = null

    abstract fun showEntry(x: T): String
    fun removeItemChangedListener() {
        listener = null
    }

    fun addOnItemChangedListener(x: OnItemChangedListener<T>) {
        listener = listener?.let { OnItemChangedListener.merge(it, x) } ?: x
    }


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

            if (multiselect) {
                handleImage.visibility = View.GONE
                multiselectCheckBox.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                settingsButton.visibility = View.GONE
                editButton.visibility = View.GONE
            } else {
                handleImage.visibility = if (enableOrder) View.VISIBLE else View.GONE
                multiselectCheckBox.visibility = View.GONE
                checkBox.visibility = View.VISIBLE
                settingsButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                initCheckBox(checkBox, item)
                initSettingsButton(settingsButton, item)
                initEditButton(editButton, item)
            }

            multiselectCheckBox.isChecked = item in selected

            if (enableAddAndDelete && removable(item)) {
                nameText.setOnLongClickListener {
                    itemTouchHelper?.startDrag(holder)
                    true
                }
                nameText.setOnClickListener {
                    select(item, multiselectCheckBox)
                }
            } else {
                multiselectCheckBox.isEnabled = false
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

    @SuppressLint("NotifyDataSetChanged")
    fun enterMultiSelect(
        onBackPressedDispatcher: OnBackPressedDispatcher,
        mainViewModel: MainViewModel
    ) {
        if (multiselect)
            return
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitMultiSelect(mainViewModel)
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback!!)
        mainViewModel.enableToolbarDeleteButton {
            deleteSelected()
            exitMultiSelect(mainViewModel)
        }
        mainViewModel.hideToolbarEditButton()
        multiselect = true
        notifyDataSetChanged()
    }


    private fun deleteSelected() {
        if (!multiselect || selected.isEmpty())
            return
        val indexed = selected.mapNotNull { entry ->
            indexItem(entry).takeIf { it != -1 }?.let { it to entry }
        }.sortedByDescending { it.first }
        indexed.forEach { (index, _) ->
            _entries.removeAt(index)
            notifyItemRemoved(index)
        }
        listener?.onItemRemovedBatch(indexed)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun exitMultiSelect(viewModel: MainViewModel) {
        if (!multiselect)
            return
        onBackPressedCallback?.remove()
        viewModel.disableToolbarDeleteButton()
        multiselect = false
        selected.clear()
        notifyDataSetChanged()
        viewModel.showToolbarEditButton()
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