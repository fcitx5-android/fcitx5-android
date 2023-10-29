/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

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
    val density: PickerPageUi.Density,
    recentlyUsedFileName: String
) : RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {

    class ViewHolder(val ui: PickerPageUi) : RecyclerView.ViewHolder(ui.root)

    /**
     * calculated layout of data in the form of
     * [(start page of the category, # of pages)]
     * Note: unlike [ranges], [pages], and [categories],
     * this does not include recently used category.
     */
    private val cats: Array<Pair<Int, Int>>

    private val ranges: List<IntRange>

    private val pages: ArrayList<Array<String>>

    /**
     * INVARIANT: The recently used category only takes one page
     * It does not interleave the layout calculation for data.
     * See the note on [cats] for details
     */
    private val recentlyUsed = RecentlyUsed(recentlyUsedFileName, density.pageSize)

    val categories: List<PickerData.Category>

    init {
        // Add recently used category
        categories = listOf(PickerData.RecentlyUsedCategory) + data.map { it.first }
        val concat = data.flatMap { it.second.toList() }
        // shift the start page of each category in data by one
        var start = 1
        var p = 0
        pages = ArrayList()
        // Add a placeholder for the recently used page
        // We will update it in [updateRecent]
        pages.add(arrayOf())
        cats = Array(data.size) { i ->
            val v = data[i].second
            val filled = v.size / density.pageSize
            val rest = v.size % density.pageSize
            val pageNum = filled + if (rest != 0) 1 else 0
            for (j in start until start + filled) {
                pages.add(j, (p until p + density.pageSize).map {
                    concat[it]
                }.toTypedArray())
                p += density.pageSize
            }
            if (rest != 0) {
                pages.add(start + pageNum - 1, (p until p + rest).map {
                    concat[it]
                }.toTypedArray())
                p += rest
            }
            (start to pageNum).also { start += pageNum }
        }
        // Add recently used page
        ranges = listOf(0..0) + cats.map { (start, pageNum) ->
            start until start + pageNum
        }
        recentlyUsed.load()
    }

    fun insertRecent(text: String) {
        if (text.length == 1 && text[0].code.let { it in Digit || it in FullWidthDigit }) return
        recentlyUsed.insert(text)
    }

    private fun updateRecent() {
        pages[0] = recentlyUsed.toOrderedList().toTypedArray()
    }

    fun saveRecent() {
        recentlyUsed.save()
    }

    fun getCategoryOfPage(page: Int) =
        ranges.indexOfFirst { page in it }

    fun getCategoryRangeOfPage(page: Int) =
        ranges.find { page in it } ?: (0..0)

    fun getStartPageOfCategory(cat: Int) =
        // Recently used category only has one page which must be the first page
        if (cat == 0)
            0
        // Otherwise, we need offset it by one
        else
            cats[cat - 1].first

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PickerPageUi(parent.context, theme, density))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ui.setItems(pages[position])
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = keyActionListener
        if (holder.bindingAdapterPosition == 0) {
            // prevent popup on RecentlyUsed page
            holder.ui.popupActionListener = null
            // update RecentlyUsed when it's page attached
            updateRecent()
            holder.ui.setItems(pages[0])
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