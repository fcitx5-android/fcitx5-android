package org.fcitx.fcitx5.android.input.candidates

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayoutManager
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.input.candidates.adapter.BaseCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.setPaddingDp
import java.lang.Integer.max

class CandidateViewBuilder :
    UniqueComponent<CandidateViewBuilder>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val theme by manager.theme()

    fun gridAdapter() = object : GridCandidateViewAdapter() {
        override val theme = this@CandidateViewBuilder.theme
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launchOnFcitxReady(fcitx) { it.select(idx) }
        }
    }

    fun flexAdapter(initLayoutParams: View.(Int) -> FlexboxLayoutManager.LayoutParams) =
        object : BaseCandidateViewAdapter() {
            override val theme = this@CandidateViewBuilder.theme
            override fun onSelect(idx: Int) {
                service.lifecycleScope.launchOnFcitxReady(fcitx) { it.select(idx) }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return super.onCreateViewHolder(parent, viewType).apply {
                    ui.root.apply {
                        setPaddingDp(10, 0, 10, 0)
                        minimumWidth = dp(40)
                    }
                }
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                // set ViewHolder's layoutParams to `FlexboxLayoutManager.LayoutParams`,
                // so layoutManager won't bother `generate{,Default}LayoutParams`
                holder.ui.root.layoutParams = initLayoutParams(holder.ui.root, position)
            }
        }

    fun dividerDrawable() = ShapeDrawable(RectShape()).apply {
        val intrinsicSize = max(1, context.dp(1))
        intrinsicWidth = intrinsicSize
        intrinsicHeight = intrinsicSize
        paint.color = theme.dividerColor
    }
}