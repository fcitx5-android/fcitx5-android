package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.ui.common.ThemeThumbnailUi
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.matchParent

open class SimpleThemeListAdapter<T : Theme>(private val entries: List<T>) :
    RecyclerView.Adapter<SimpleThemeListAdapter.ViewHolder>() {

    class ViewHolder(val ui: Ui) : RecyclerView.ViewHolder(ui.root)

    var selected = -1
        set(value) {
            val last = field
            field = value
            if (last != -1)
                notifyItemChanged(last)
            if (field != -1)
                notifyItemChanged(field)
        }

    val selectedTheme
        get() = selected.takeIf { it != -1 }?.let { entries[it] }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ThemeThumbnailUi(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.ui as ThemeThumbnailUi).apply {
            root.layoutParams = ViewGroup.LayoutParams(root.matchParent, root.dp(150))
            editButton.visibility = View.GONE
            val theme = entries[position]
            setTheme(theme)
            setChecked(position == selected)
            root.setOnClickListener {
                onClick(theme)
                selected = position
            }
        }
    }

    override fun getItemCount(): Int = entries.size

    open fun onClick(theme: T) {}
}