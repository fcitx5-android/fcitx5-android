/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
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
import java.util.Collections

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

    fun removeItemChangedListener() {
        listener = null
    }

    fun addOnItemChangedListener(x: OnItemChangedListener<T>) {
        listener = listener?.let { OnItemChangedListener.merge(it, x) } ?: x
    }

    protected var itemTouchHelper: ItemTouchHelper? = null

    var removable: (T) -> Boolean = { true }

    private var onBackPressedCallback: OnBackPressedCallback? = null

    abstract fun showEntry(x: T): String

    private var mainViewModel: MainViewModel? = null

    fun setViewModel(model: MainViewModel) {
        mainViewModel = model
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
                multiselectCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    select(item, isChecked)
                }
                nameText.setOnLongClickListener {
                    itemTouchHelper?.startDrag(holder)
                    true
                }
                nameText.setOnClickListener {
                    multiselectCheckBox.toggle()
                }
            } else {
                multiselectCheckBox.isEnabled = false
                multiselectCheckBox.setOnCheckedChangeListener(null)
                nameText.setOnClickListener(null)
            }
        }
    }

    private fun select(item: T, shouldSelect: Boolean) {
        if (!enableAddAndDelete || !multiselect)
            return
        if (shouldSelect) {
            if (selected.indexOf(item) == -1) selected.add(item)
        } else {
            selected.remove(item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @CallSuper
    open fun enterMultiSelect(onBackPressedDispatcher: OnBackPressedDispatcher) {
        mainViewModel?.let {
            if (multiselect)
                return
            onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    exitMultiSelect()
                }
            }
            onBackPressedDispatcher.addCallback(onBackPressedCallback!!)
            it.enableToolbarDeleteButton {
                deleteSelected()
                exitMultiSelect()
            }
            it.hideToolbarEditButton()
            multiselect = true
            notifyDataSetChanged()
        }
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
    @CallSuper
    open fun exitMultiSelect() {
        mainViewModel?.let {
            if (!multiselect)
                return
            onBackPressedCallback?.remove()
            it.disableToolbarDeleteButton()
            multiselect = false
            selected.clear()
            notifyDataSetChanged()
            if (entries.isNotEmpty())
                it.showToolbarEditButton()
        }
    }

    override fun getItemCount(): Int = entries.size

    @CallSuper
    open fun addItem(idx: Int = _entries.size, item: T) {
        _entries.add(idx, item)
        notifyItemInserted(idx)
        listener?.onItemAdded(idx, item)
        mainViewModel?.showToolbarEditButton()
    }

    @CallSuper
    open fun removeItem(idx: Int): T {
        val item = _entries.removeAt(idx)
        notifyItemRemoved(idx)
        listener?.onItemRemoved(idx, item)
        if (entries.isEmpty())
            mainViewModel?.hideToolbarEditButton()
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