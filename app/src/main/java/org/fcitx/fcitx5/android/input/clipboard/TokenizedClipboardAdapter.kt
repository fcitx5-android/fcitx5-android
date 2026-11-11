/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp

class TokenizedClipboardAdapter(
    private val theme: Theme,
    private val onSelectionChanged: (selectedCount: Int, totalCount: Int) -> Unit
) : RecyclerView.Adapter<TokenizedClipboardAdapter.ViewHolder>() {

    class ViewHolder(val tokenUi: TokenizedClipboardTokenUi) : RecyclerView.ViewHolder(tokenUi.root)

    private var tokens: List<ClipboardToken> = emptyList()
    private val selectedIndices = linkedSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TokenizedClipboardTokenUi(parent.context, theme)).apply {
            itemView.layoutParams = FlexboxLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(parent.context.dp(3), parent.context.dp(3), parent.context.dp(3), parent.context.dp(3))
            }
        }
    }

    override fun getItemCount(): Int = tokens.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = tokens[position]
        holder.tokenUi.setToken(token.text, position in selectedIndices)
        holder.itemView.setOnClickListener {
            toggleSelection(position)
        }
    }

    fun submitTokens(newTokens: List<ClipboardToken>) {
        tokens = newTokens
        selectedIndices.clear()
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun toggleSelectAll() {
        if (tokens.isEmpty()) return
        if (selectedIndices.size == tokens.size) {
            selectedIndices.clear()
        } else {
            selectedIndices.clear()
            selectedIndices.addAll(tokens.indices)
        }
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun clearSelection() {
        if (selectedIndices.isEmpty()) return
        selectedIndices.clear()
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun invertSelection() {
        if (tokens.isEmpty()) return
        val previous = selectedIndices.toSet()
        selectedIndices.clear()
        tokens.indices.forEach { index ->
            if (index !in previous) {
                selectedIndices.add(index)
            }
        }
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun selectedTokens(): List<ClipboardToken> =
        selectedIndices.sorted().map { tokens[it] }

    private fun toggleSelection(index: Int) {
        if (index !in tokens.indices) return
        if (!selectedIndices.add(index)) {
            selectedIndices.remove(index)
        }
        notifyItemChanged(index)
        dispatchSelectionChanged()
    }

    private fun dispatchSelectionChanged() {
        onSelectionChanged(selectedIndices.size, tokens.size)
    }
}
