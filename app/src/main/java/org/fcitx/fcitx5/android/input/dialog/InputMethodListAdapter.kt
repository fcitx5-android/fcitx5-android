/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.dialog

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class InputMethodListAdapter(
    val entries: List<InputMethodData>,
    private var enabledIndex: Int,
    private val onEntryClick: (InputMethodData) -> Unit
) : RecyclerView.Adapter<InputMethodListAdapter.Holder>() {

    inner class Holder(val ui: InputMethodEntryUi) : RecyclerView.ViewHolder(ui.root)

    override fun getItemCount() = entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(InputMethodEntryUi(parent.context))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ime = entries[position]
        holder.ui.root.apply {
            isChecked = position == enabledIndex
            text = ime.name
            setOnClickListener { onEntryClick(ime) }
        }
    }

    fun setEnabled(position: Int) {
        if (position == enabledIndex) return
        notifyItemChanged(enabledIndex)
        enabledIndex = position
        notifyItemChanged(position)
    }
}
