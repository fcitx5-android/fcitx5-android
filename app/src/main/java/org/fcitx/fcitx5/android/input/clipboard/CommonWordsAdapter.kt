/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.DeviceUtil
import org.fcitx.fcitx5.android.utils.item
import splitties.resources.styledColor

abstract class CommonWordsAdapter(
    private val theme: Theme,
    private val entryRadius: Float
) : RecyclerView.Adapter<CommonWordsAdapter.ViewHolder>() {

    private var entries: List<QuickPhraseEntry> = emptyList()
    private var popupMenu: PopupMenu? = null

    class ViewHolder(val ui: CommonWordEntryUi) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(CommonWordEntryUi(parent.context, theme, entryRadius))

    override fun getItemCount() = entries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.ui.apply {
            setEntry(entry)
            root.setOnClickListener { onPaste(entry) }
            root.setOnLongClickListener {
                val popup = PopupMenu(ctx, root)
                val iconTint = ctx.styledColor(android.R.attr.colorControlNormal)
                popup.menu.item(R.string.edit, R.drawable.ic_baseline_edit_24, iconTint) {
                    onEdit()
                }
                popup.menu.item(R.string.delete, R.drawable.ic_baseline_delete_24, iconTint) {
                    onDelete(entry)
                }
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !DeviceUtil.isSamsungOneUI &&
                    !DeviceUtil.isFlyme
                ) {
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

    @SuppressLint("NotifyDataSetChanged")
    fun updateEntries(newEntries: List<QuickPhraseEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    fun onDetached() {
        popupMenu?.dismiss()
        popupMenu = null
    }

    abstract fun onPaste(entry: QuickPhraseEntry)

    abstract fun onEdit()

    abstract fun onDelete(entry: QuickPhraseEntry)
}
