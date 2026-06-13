/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
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
    private val rawData: List<Pair<PickerData.Category, Array<String>>>,
    private val density: PickerPageUi.Density,
    recentlyUsedFileName: String,
    private val bordered: Boolean,
    private val policy: PickerPolicy
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

    private fun buildCategories(data: List<Pair<PickerData.Category, Array<String>>>) {
        data.forEach { (cat, arr) ->
            val list = arr.filter(policy::filter)
            val chunks = list.chunked(density.pageSize)
            categories.add(cat to IntRange(pages.size, pages.size + chunks.size - 1))
            pages.addAll(chunks)
        }
    }

    init {
        buildCategories(rawData)
    }

    private fun rebuildCategories() {
        categories.clear()
        // empty "RecentlyUsed" category
        categories.add(PickerData.RecentlyUsedCategory to IntRange(0, 0))
        pages.clear()
        // empty "RecentlyUsed" page
        pages.add(emptyList())
        buildCategories(rawData)
    }

    private var lastInvalidateKey = policy.invalidateKey()

    @SuppressLint("NotifyDataSetChanged")
    fun refreshIfNeeded() {
        val newKey = policy.invalidateKey()
        if (lastInvalidateKey != newKey) {
            lastInvalidateKey = newKey
            rebuildCategories()
            notifyDataSetChanged()
        }
    }

    private val recentlyUsed = RecentlyUsed(recentlyUsedFileName, density.pageSize)

    fun insertRecent(text: String) {
        if (text.length == 1 && text[0].code.let { it in Digit || it in FullWidthDigit }) return
        recentlyUsed.insert(text)
    }

    fun getCategoryList(): List<PickerData.Category> {
        return categories.map { it.first }
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
        if (position == 0) {
            // RecentlyUsed content should be displayed as-is, without popups
            holder.ui.setItems(recentlyUsed.items)
        } else {
            holder.ui.setItems(pages[position], policy)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = keyActionListener
        holder.ui.popupActionListener = if (holder.bindingAdapterPosition == 0) {
            // prevent popup on RecentlyUsed page
            null
        } else {
            popupActionListener
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