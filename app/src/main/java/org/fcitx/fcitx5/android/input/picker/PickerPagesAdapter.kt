package org.fcitx.fcitx5.android.input.picker

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener

class PickerPagesAdapter(
    val theme: Theme,
    private val keyActionListener: KeyActionListener,
    data: List<Pair<String, Array<String>>>
) :
    RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {
    class ViewHolder(val ui: PickerPageUi) : RecyclerView.ViewHolder(ui.root)

    private val cats: Array<Pair<Int, Int>>
    private val ranges: List<IntRange>
    val pages: List<List<String>>

    val categories: List<String>

    init {
        categories = data.map { it.first }
        val concat = data.flatMap { it.second.toList() }
        var start = 0
        var p = 0
        pages = ArrayList()
        cats = Array(data.size) { i ->
            val v = data[i].second
            val filled = v.size / ITEMS_PER_PAGE
            val rest = v.size % ITEMS_PER_PAGE
            val pageNum = filled + if (rest != 0) 1 else 0
            for (j in start until start + filled) {
                pages.add(j, (p until p + ITEMS_PER_PAGE).map { concat[it] })
                p += ITEMS_PER_PAGE
            }
            if (rest != 0) {
                pages.add(start + pageNum - 1, (p until p + rest).map { concat[it] })
                p += rest
            }
            (start to pageNum).also { start += pageNum }
        }
        ranges = cats.map { (start, pageNum) ->
            start until start + pageNum
        }
    }

    fun getCategoryOfPage(page: Int) =
        ranges.indexOfFirst { page in it }

    fun getStartPageOfCategory(cat: Int) =
        cats[cat].first

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PickerPageUi(parent.context, theme))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ui.setItems(pages[position])
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