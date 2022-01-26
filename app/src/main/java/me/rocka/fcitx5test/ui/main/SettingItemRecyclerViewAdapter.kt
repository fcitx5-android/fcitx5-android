package me.rocka.fcitx5test.ui.main

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledDrawable
import splitties.views.dsl.core.*
import splitties.views.textAppearance

class SettingItemRecyclerViewAdapter(
    private vararg val settingItems: Pair<String, () -> Unit>
) : RecyclerView.Adapter<SettingItemRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(val rootView: View, val textView: TextView) :
        RecyclerView.ViewHolder(rootView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        with(parent.context) {
            val textView = textView {
                textAppearance = resolveThemeAttribute(R.attr.textAppearanceListItem)
                layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
            }
            ViewHolder(
                horizontalLayout {
                    background = styledDrawable(android.R.attr.selectableItemBackground)
                    layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                    add(textView, lParams { margin = dp(16) })
                },
                textView
            )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (string, action) = settingItems[position]
        holder.run {
            textView.text = string
            rootView.setOnClickListener { action() }
        }
    }

    override fun getItemCount(): Int = settingItems.size

}