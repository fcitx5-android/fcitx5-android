package org.fcitx.fcitx5.android.input.candidates.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import splitties.dimensions.dp
import splitties.views.dsl.core.wrapContent

abstract class SimpleCandidateViewAdapter : BaseCandidateViewAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            ui.root.apply {
                layoutParams = GridLayoutManager.LayoutParams(wrapContent, dp(40))
            }
        }
    }
}