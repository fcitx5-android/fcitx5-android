package org.fcitx.fcitx5.android.input.picker

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener

class PickerPagesAdapter(
    val theme: Theme,
    private val keyActionListener: KeyActionListener,
    data: Map<String, Array<String>>
) :
    RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {
    class ViewHolder(val ui: PickerPageUi) : RecyclerView.ViewHolder(ui.root)

    val categories = data.keys.toList()
    val categoryOfPosition: List<Int>
    val positionOfCategory: List<Int>

    // TODO: configurable page size
    val pages: List<List<String>>

    init {
        val _categoryOfPosition = ArrayList<Int>(categories.size)
        val _positionOfCategory = ArrayList<Int>(categories.size)
        val _pages = ArrayList<List<String>>(categories.size)
        var categoryCount = 0
        data.forEach { (category, items) ->
            items.toList().chunked(28).let {
                repeat(it.size) { _categoryOfPosition.add(categoryCount) }
                _positionOfCategory.add(_pages.size)
                _pages.addAll(it)
                categoryCount++
            }
        }
        pages = _pages
        categoryOfPosition = _categoryOfPosition
        positionOfCategory = _positionOfCategory
    }

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
}