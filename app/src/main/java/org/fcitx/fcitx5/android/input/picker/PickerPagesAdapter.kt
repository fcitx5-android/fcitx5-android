package org.fcitx.fcitx5.android.input.picker

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme

class PickerPagesAdapter(val theme: Theme) :
    RecyclerView.Adapter<PickerPagesAdapter.ViewHolder>() {
    class ViewHolder(val root: PickerPageView) : RecyclerView.ViewHolder(root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PickerPageView(parent.context, theme))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.root.tv.text = "$position"
    }

    // TODO: should be count of categories, or pages?
    override fun getItemCount() = 7
}