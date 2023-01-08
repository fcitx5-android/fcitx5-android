package org.fcitx.fcitx5.android.input.clipboard

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.resources.drawable
import splitties.resources.styledColor
import kotlin.collections.set
import kotlin.math.min

abstract class ClipboardAdapter :
    RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    companion object {
        /**
         * excerpt text to show on ClipboardEntryUi, to reduce render time of very long text
         * @param str text to excerpt
         * @param lines max output lines
         * @param chars max chars per output line
         */
        fun excerptText(str: String, lines: Int = 4, chars: Int = 128) = buildString {
            val totalLength = str.length
            var lineBreak = -1
            for (i in 1..lines) {
                val start = lineBreak + 1   // skip previous '\n'
                lineBreak = str.indexOf('\n', start)
                if (lineBreak < 0) {
                    // no line breaks remaining, substring to end of text
                    append(str.substring(start, min(start + chars, totalLength)))
                    break
                } else {
                    // append one line exactly
                    appendLine(str.substring(start, min(min(lineBreak, start + chars), totalLength)))
                }
            }
        }
    }

    private val _entries = mutableListOf<ClipboardEntry>()

    // maps entry id to list index
    // since we don't have much data, we are not using sparse int array here
    private val _entriesId = mutableMapOf<Int, Int>()

    val entries: List<ClipboardEntry>
        get() = _entries

    fun getPositionById(id: Int) = _entriesId.getValue(id)

    fun getEntryById(id: Int) = entries[getPositionById(id)]

    private var popupMenu: PopupMenu? = null

    inner class ViewHolder(val entryUi: ClipboardEntryUi) :
        RecyclerView.ViewHolder(entryUi.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardEntryUi(parent.context, theme))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.entryUi) {
            val entry = _entries[position]
            text.text = excerptText(entry.text)
            pin.visibility = if (entry.pinned) View.VISIBLE else View.INVISIBLE
            root.setOnClickListener {
                onPaste(entry.id)
            }
            root.setOnLongClickListener {
                popupMenu?.dismiss()
                val iconColor = ctx.styledColor(android.R.attr.colorControlNormal)
                val iconColorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                val popup = PopupMenu(root.context, root)
                val scope = root.findViewTreeLifecycleOwner()!!.lifecycleScope
                fun menuItem(@StringRes title: Int, @DrawableRes ic: Int, cb: suspend () -> Unit) {
                    popup.menu.add(title).apply {
                        icon = ctx.drawable(ic)?.apply { colorFilter = iconColorFilter }
                        setOnMenuItemClickListener {
                            scope.launch { cb() }
                            true
                        }
                    }
                }
                if (entry.pinned) menuItem(R.string.unpin, R.drawable.ic_outline_push_pin_24) {
                    onUnpin(entry.id)
                    setPinStatus(entry.id, false)
                } else menuItem(R.string.pin, R.drawable.ic_baseline_push_pin_24) {
                    onPin(entry.id)
                    setPinStatus(entry.id, true)
                }
                menuItem(R.string.edit, R.drawable.ic_baseline_edit_24) {
                    onEdit(entry.id)
                }
                menuItem(R.string.delete, R.drawable.ic_baseline_delete_24) {
                    delete(entry.id)
                    // make `onDelete` access entries after delete
                    onDelete(entry.id)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popup.setForceShowIcon(true)
                }
                popupMenu = popup
                popup.setOnDismissListener {
                    if (it === popupMenu) popupMenu = null
                }
                popup.show()
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
                else -> o2.timestamp.compareTo(o1.timestamp)
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

    fun onDetached() {
        popupMenu?.dismiss()
    }

    abstract val theme: Theme

    override fun getItemCount(): Int = _entries.size

    abstract fun onPaste(id: Int)

    abstract suspend fun onPin(id: Int)

    abstract suspend fun onUnpin(id: Int)

    abstract fun onEdit(id: Int)

    abstract suspend fun onDelete(id: Int)

}