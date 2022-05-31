package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.matchParent

abstract class BuiltinThemeListAdapter :
    RecyclerView.Adapter<BuiltinThemeListAdapter.ViewHolder>() {

    class ViewHolder(val ui: Ui) : RecyclerView.ViewHolder(ui.root)

    private val entries = ThemeManager.builtinThemes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ThemeThumbnailUi(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.ui as ThemeThumbnailUi).apply {
            root.layoutParams = ViewGroup.LayoutParams(root.matchParent, root.dp(150))
            val theme = entries[position]
            setTheme(theme)
            root.setOnClickListener { onClick(theme) }
        }
    }

    override fun getItemCount(): Int = entries.size

    abstract fun onClick(theme: Theme.Builtin)
}