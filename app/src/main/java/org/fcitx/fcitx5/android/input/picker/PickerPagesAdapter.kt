package org.fcitx.fcitx5.android.input.picker

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import arrow.core.flatten
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener

class PickerPagesAdapter(
    val theme: Theme,
    private val keyActionListener: KeyActionListener,
    data: Map<String, Array<String>>
) :
    RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {
    class ViewHolder(val ui: PickerPageUi) : RecyclerView.ViewHolder(ui.root)

    // TODO: configurable page size
    val pages = data.values.map { it.toList().chunked(28) }.flatten()

    // TODO: should be count of categories, or pages?
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