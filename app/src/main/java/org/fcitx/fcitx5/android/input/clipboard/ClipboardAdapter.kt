/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.os.Build
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.DeviceUtil
import splitties.resources.drawable
import splitties.resources.styledColor
import kotlin.math.min

abstract class ClipboardAdapter :
    PagingDataAdapter<ClipboardEntry, ClipboardAdapter.ViewHolder>(diffCallback) {

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ClipboardEntry>() {
            override fun areItemsTheSame(
                oldItem: ClipboardEntry,
                newItem: ClipboardEntry
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ClipboardEntry,
                newItem: ClipboardEntry
            ): Boolean {
                return oldItem == newItem
            }
        }

        /**
         * excerpt text to show on ClipboardEntryUi, to reduce render time of very long text
         * @param str text to excerpt
         * @param lines max output lines
         * @param chars max chars per output line
         */
        fun excerptText(str: String, lines: Int = 4, chars: Int = 128) = buildString {
            val length = str.length
            var lineBreak = -1
            for (i in 1..lines) {
                val start = lineBreak + 1   // skip previous '\n'
                val excerptEnd = min(start + chars, length)
                lineBreak = str.indexOf('\n', start)
                if (lineBreak < 0) {
                    // no line breaks remaining, substring to end of text
                    append(str.substring(start, excerptEnd))
                    break
                } else {
                    // append one line exactly
                    appendLine(str.substring(start, min(excerptEnd, lineBreak)))
                }
            }
        }
    }

    private var popupMenu: PopupMenu? = null

    class ViewHolder(val entryUi: ClipboardEntryUi) : RecyclerView.ViewHolder(entryUi.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardEntryUi(parent.context, theme))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position) ?: return
        with(holder.entryUi) {
            setEntry(excerptText(entry.text), entry.pinned)
            root.setOnClickListener {
                onPaste(entry)
            }
            root.setOnLongClickListener {
                val iconColor = ctx.styledColor(android.R.attr.colorControlNormal)
                val popup = PopupMenu(ctx, root)
                fun menuItem(@StringRes title: Int, @DrawableRes ic: Int, callback: () -> Unit) {
                    popup.menu.add(title).apply {
                        icon = ctx.drawable(ic)?.apply { setTint(iconColor) }
                        setOnMenuItemClickListener {
                            callback()
                            true
                        }
                    }
                }
                if (entry.pinned) menuItem(R.string.unpin, R.drawable.ic_outline_push_pin_24) {
                    onUnpin(entry.id)
                } else menuItem(R.string.pin, R.drawable.ic_baseline_push_pin_24) {
                    onPin(entry.id)
                }
                menuItem(R.string.edit, R.drawable.ic_baseline_edit_24) {
                    onEdit(entry.id)
                }
                menuItem(R.string.delete, R.drawable.ic_baseline_delete_24) {
                    onDelete(entry.id)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !DeviceUtil.isSamsungOneUI) {
                    popup.setForceShowIcon(true)
                }
                popup.setOnDismissListener {
                    if (it === popupMenu) popupMenu = null
                }
                popupMenu?.dismiss()
                popupMenu = popup
                popup.show()
                true
            }
        }
    }

    fun getEntryAt(position: Int) = getItem(position)

    fun onDetached() {
        popupMenu?.dismiss()
        popupMenu = null
    }

    abstract val theme: Theme

    abstract fun onPaste(entry: ClipboardEntry)

    abstract fun onPin(id: Int)

    abstract fun onUnpin(id: Int)

    abstract fun onEdit(id: Int)

    abstract fun onDelete(id: Int)

}