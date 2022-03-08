package me.rocka.fcitx5test.keyboard.clipboard

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.clipboard.db.ClipboardEntry
import kotlin.collections.set

abstract class ClipboardAdapter(initEntries: List<ClipboardEntry>) :
    RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {
    private val _entries = mutableListOf<ClipboardEntry>()

    // maps entry id to list index
    // since we don't have much data, we are not using sparse int array here
    private val _entriesId = mutableMapOf<Int, Int>()

    val entries: List<ClipboardEntry>
        get() = _entries

    init {
        updateEntries(initEntries)
    }

    fun getPositionById(id: Int) = _entriesId.getValue(id)

    fun getEntryById(id: Int) = entries[getPositionById(id)]

    inner class ViewHolder(val entryUi: ClipboardUi) :
        RecyclerView.ViewHolder(entryUi.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardUi(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.entryUi) {
            val entry = _entries[position]
            text.text = entry.text
            pin.visibility = if (entry.pinned) View.VISIBLE else View.INVISIBLE
            root.setOnClickListener {
                onPaste(entry.id)
            }
            root.setOnLongClickListener {
                val menu = PopupMenu(root.context, root)
                val scope = root.findViewTreeLifecycleOwner()!!.lifecycleScope
                menu.menuInflater.inflate(R.menu.clipboard_management_menu, menu.menu)
                menu.menu.findItem(R.id.clipboard_management_pin).apply {
                    isVisible = !entry.pinned
                    setOnMenuItemClickListener {
                        scope.launch {
                            onPin(entry.id)
                            setPinStatus(entry.id, true)
                        }
                        true
                    }
                }
                menu.menu.findItem(R.id.clipboard_management_unpin).apply {
                    isVisible = entry.pinned
                    setOnMenuItemClickListener {
                        scope.launch {
                            onUnpin(entry.id)
                            setPinStatus(entry.id, false)
                        }
                        true
                    }
                }
                menu.menu.findItem(R.id.clipboard_management_delete).apply {
                    isVisible = true
                    setOnMenuItemClickListener {
                        scope.launch {
                            onDelete(entry.id)
                            delete(entry.id)
                        }
                        true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    menu.setForceShowIcon(true)
                }
                menu.show()
                true
            }
        }
    }

    private fun delete(id: Int) {
        val position = _entriesId.getValue(id)
        _entries.removeAt(position)
        _entriesId.remove(id)
        // Update indices after the removed item
        for (i in position until _entries.size) {
            _entriesId[_entries[i].id] = i
        }
        notifyItemRemoved(position)
    }

    private fun setPinStatus(id: Int, pinned: Boolean) {
        val position = _entriesId.getValue(id)
        _entries[position] = _entries[position].copy(pinned = pinned)
        notifyItemChanged(position)
        // pin will cause a change of order
        updateEntries(_entries)
    }

    fun updateEntries(entries: List<ClipboardEntry>) {
        val sorted = entries.sortedWith { o1, o2 ->
            when {
                o1.pinned && !o2.pinned -> -1
                !o1.pinned && o2.pinned -> 1
                else -> o2.id.compareTo(o1.id)
            }
        }
        val callback = ClipboardEntryDiffCallback(_entries, sorted)
        val diff = DiffUtil.calculateDiff(callback)
        _entries.clear()
        _entries.addAll(sorted)
        _entriesId.clear()
        _entries.forEachIndexed { index, clipboardEntry ->
            _entriesId[clipboardEntry.id] = index
        }
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = _entries.size

    abstract fun onPaste(id: Int)

    abstract suspend fun onPin(id: Int)

    abstract suspend fun onUnpin(id: Int)

    abstract suspend fun onDelete(id: Int)

}