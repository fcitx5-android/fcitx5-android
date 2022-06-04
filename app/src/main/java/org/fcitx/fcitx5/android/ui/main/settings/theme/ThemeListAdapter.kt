package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.dsl.core.Ui

abstract class ThemeListAdapter : RecyclerView.Adapter<ThemeListAdapter.ViewHolder>() {
    class ViewHolder(val ui: Ui) : RecyclerView.ViewHolder(ui.root)

    val entries = mutableListOf<Theme>()

    private var checkedIndex = -1

    private fun entryAt(position: Int) = entries.getOrNull(position - OFFSET)

    private fun positionOf(theme: Theme) = entries.indexOfFirst { it.name == theme.name } + OFFSET

    fun setThemes(themes: List<Theme>, active: Theme) {
        entries.clear()
        entries.addAll(themes)
        checkedIndex = entries.indexOf(active) + OFFSET
        notifyItemRangeInserted(OFFSET, themes.size)
    }

    fun setCheckedTheme(theme: Theme) {
        val oldChecked = entryAt(checkedIndex)
        if (oldChecked == theme) return
        notifyItemChanged(checkedIndex)
        checkedIndex = positionOf(theme)
        notifyItemChanged(checkedIndex)
    }

    fun prependTheme(it: Theme) {
        entries.add(0, it)
        checkedIndex += 1
        notifyItemInserted(OFFSET)
    }

    fun replaceTheme(theme: Theme) {
        val index = entries.indexOfFirst { it.name == theme.name }
        entries[index] = theme
        notifyItemChanged(index + OFFSET)
    }

    fun removeTheme(name: String) {
        val index = entries.indexOfFirst { it.name == name }
        entries.removeAt(index)
        notifyItemRemoved(index + OFFSET)
        val cmp = (index - OFFSET).compareTo(checkedIndex)
        when {
            cmp > 0 -> {
                // Do nothing
            }
            cmp == 0 -> {
                // Reset
                checkedIndex = -1
            }
            cmp < 0 -> {
                // Fix
                checkedIndex -= 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            when (viewType) {
                ADD_THEME -> NewThemeEntryUi(parent.context)
                THEME -> ThemeThumbnailUi(parent.context)
                else -> throw IllegalArgumentException(INVALID_TYPE + viewType)
            }
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val it = getItemViewType(position)) {
            ADD_THEME -> holder.ui.root.setOnClickListener { onAddNewTheme() }
            THEME -> (holder.ui as ThemeThumbnailUi).apply {
                val theme = entryAt(position)!!
                val isActive = position == checkedIndex
                setTheme(theme, isActive)
                root.setOnClickListener {
                    onSelectTheme(theme)
                }
                root.setOnLongClickListener {
                    if (theme is Theme.Custom) {
                        onExportTheme(theme)
                        true
                    } else false
                }
                editButton.setOnClickListener {
                    if (theme is Theme.Custom) onEditTheme(theme)
                }
            }
            else -> throw IllegalArgumentException(INVALID_TYPE + it)
        }
    }

    override fun getItemCount() = entries.size + 1

    override fun getItemViewType(position: Int) = if (position == 0) ADD_THEME else THEME

    abstract fun onAddNewTheme()

    abstract fun onSelectTheme(theme: Theme)

    abstract fun onEditTheme(theme: Theme.Custom)

    abstract fun onExportTheme(theme: Theme.Custom)

    companion object {
        const val OFFSET = 1

        const val ADD_THEME = 0
        const val THEME = 1

        const val INVALID_TYPE = "Invalid ItemView Type: "
    }
}
