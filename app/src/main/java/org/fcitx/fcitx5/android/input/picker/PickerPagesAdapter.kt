package org.fcitx.fcitx5.android.input.picker

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.RecentlyUsed
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener

class PickerPagesAdapter(
    val theme: Theme,
    private val keyActionListener: KeyActionListener,
    data: List<Pair<String, Array<String>>>
) :
    RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {
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
    private val recentlyUsed = RecentlyUsed("picker", ITEMS_PER_PAGE)

    val categories: List<String>

    init {
        // Add recently used category
        categories = listOf(PickerPreset.RecentlyUsedSymbol) + data.map { it.first }
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
            val filled = v.size / ITEMS_PER_PAGE
            val rest = v.size % ITEMS_PER_PAGE
            val pageNum = filled + if (rest != 0) 1 else 0
            for (j in start until start + filled) {
                pages.add(j, (p until p + ITEMS_PER_PAGE).map {
                    concat[it]
                }.toTypedArray())
                p += ITEMS_PER_PAGE
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

    fun updateRecent() {
        pages[0] = recentlyUsed.toOrderedList().toTypedArray()
        notifyItemChanged(0)
    }

    fun saveRecent() {
        recentlyUsed.save()
    }

    fun getCategoryOfPage(page: Int) =
        ranges.indexOfFirst { page in it }

    fun getStartPageOfCategory(cat: Int) =
        // Recently used category only has one page which must be the first page
        if (cat == 0)
            0
        // Otherwise, we need offset it by one
        else
            cats[cat - 1].first

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PickerPageUi(parent.context, theme))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ui.setItems(pages[position]) {
            recentlyUsed.insert(it)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = keyActionListener
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.ui.keyActionListener = null
    }

    companion object {
        private const val ITEMS_PER_PAGE = 28
    }
}