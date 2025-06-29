/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.text.TextPaint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.RecentlyUsed
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.popup.PopupActionListener

class PickerPagesAdapter(
    val theme: Theme,
    private val keyActionListener: KeyActionListener,
    private val popupActionListener: PopupActionListener,
    data: List<Pair<PickerData.Category, Array<String>>>,
    private val density: PickerPageUi.Density,
    recentlyUsedFileName: String,
    private val bordered: Boolean = false,
    private val isEmoji: Boolean = false
) : RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {

    class ViewHolder(val ui: PickerPageUi) : RecyclerView.ViewHolder(ui.root)

    /**
     * list<`Category` to `[start, end]`>, starting with empty "RecentlyUsed" category
     */
    private val categories: MutableList<Pair<PickerData.Category, IntRange>> = mutableListOf(
        PickerData.RecentlyUsedCategory to IntRange(0, 0)
    )

    /**
     * list<page of symbols>, starting with empty "RecentlyUsed" page
     */
    private val pages: MutableList<List<String>> = mutableListOf(listOf())

    fun getCategoryList(): List<PickerData.Category> {
        return categories.map { it.first }
    }

    init {
        val textPaint = TextPaint()
        data.forEach { (cat, arr) ->
            val chunks = arr.filter { if (isEmoji) textPaint.hasGlyph(it) else true }
                .chunked(density.pageSize)
            categories.add(cat to IntRange(pages.size, pages.size + chunks.size - 1))
            pages.addAll(chunks)
        }
    }

    private val recentlyUsed = RecentlyUsed(recentlyUsedFileName, density.pageSize)

    fun insertRecent(text: String) {
        if (text.length == 1 && text[0].code.let { it in Digit || it in FullWidthDigit }) return
        recentlyUsed.insert(text)
    }

    fun getCategoryIndexOfPage(page: Int): Int {
        return categories.indexOfFirst { page in it.second }
    }

    fun getCategoryRangeOfPage(page: Int): IntRange {
        return categories.find { page in it.second }?.second ?: IntRange(0, 0)
    }

    fun getRangeOfCategoryIndex(cat: Int): IntRange {
        return categories[cat].second
    }

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PickerPageUi(parent.context, theme, density, bordered))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ui.setItems(pages[position], isEmoji)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = keyActionListener
        if (holder.bindingAdapterPosition == 0) {
            // prevent popup on RecentlyUsed page
            holder.ui.popupActionListener = null
            // RecentlyUsed content are already modified with skin tones
            holder.ui.setItems(recentlyUsed.items, withSkinTone = false)
        } else {
            holder.ui.popupActionListener = popupActionListener
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = null
        holder.ui.popupActionListener = null
    }

    companion object {
        private val Digit = IntRange('0'.code, '9'.code)
        private val FullWidthDigit = IntRange('０'.code, '９'.code)
    }
}