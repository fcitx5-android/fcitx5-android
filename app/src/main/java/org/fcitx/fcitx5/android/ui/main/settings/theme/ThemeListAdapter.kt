package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import splitties.views.dsl.core.Ui

abstract class ThemeListAdapter : RecyclerView.Adapter<ThemeListAdapter.ViewHolder>() {
    class ViewHolder(val ui: Ui) : RecyclerView.ViewHolder(ui.root)

    val entries = mutableListOf<Theme>()

    private fun entryAt(position: Int) = entries[position - OFFSET]

    fun setThemes(themes: List<Theme>) {
        entries.clear()
        entries.addAll(themes)
        notifyItemRangeInserted(OFFSET, themes.size)
    }

    fun prependTheme(it: Theme) {
        entries.add(0, it)
        notifyItemInserted(OFFSET)
    }

    fun replaceTheme(theme: Theme) {
        val index = entries.indexOfFirst { it.name.contentEquals(theme.name) }
        entries[index] = theme
        notifyItemChanged(index + OFFSET)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            when (viewType) {
                CHOOSE_IMAGE -> ChooseImageEntryUi(parent.context)
                THEME -> ThemeThumbnailUi(parent.context)
                else -> throw IllegalArgumentException(INVALID_TYPE + viewType)
            }
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val it = getItemViewType(position)) {
            CHOOSE_IMAGE -> holder.ui.root.setOnClickListener { onChooseImage() }
            THEME -> (holder.ui as ThemeThumbnailUi).apply {
                entryAt(position).let { theme ->
                    setTheme(theme, theme.name.contentEquals(ThemeManager.currentTheme.name))
                    root.setOnClickListener { onSelectTheme(theme) }
                    editButton.setOnClickListener {
                        if (theme is Theme.Custom) onEditTheme(theme)
                    }
                }
            }
            else -> throw IllegalArgumentException(INVALID_TYPE + it)
        }
    }

    override fun getItemCount() = entries.size + 1

    override fun getItemViewType(position: Int) = if (position == 0) CHOOSE_IMAGE else THEME

    abstract fun onChooseImage()

    abstract fun onSelectTheme(theme: Theme)

    abstract fun onEditTheme(theme: Theme.Custom)

    companion object {
        const val OFFSET = 1

        const val CHOOSE_IMAGE = 0
        const val THEME = 1

        const val INVALID_TYPE = "Invalid ItemView Type: "
    }
}
