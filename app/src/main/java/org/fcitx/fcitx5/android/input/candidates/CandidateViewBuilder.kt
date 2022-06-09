package org.fcitx.fcitx5.android.input.candidates

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.*
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.SimpleCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.SpanHelper
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxHorizontalDecoration
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxVerticalDecoration
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.GridDecoration
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp

class CandidateViewBuilder : UniqueComponent<CandidateViewBuilder>(), Dependent,
    ManagedHandler by managedHandler() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val fcitx: Fcitx by manager.fcitx()
    private val theme by manager.theme()

    fun gridAdapter() = object : GridCandidateViewAdapter() {
        override fun onTouchDown(view: View) {
            view.hapticIfEnabled()
        }

        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }

        override val theme: Theme
            get() = this@CandidateViewBuilder.theme
    }

    fun simpleAdapter() = object : SimpleCandidateViewAdapter() {
        override fun onTouchDown(view: View) {
            view.hapticIfEnabled()
        }

        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }

        override val theme: Theme
            get() = this@CandidateViewBuilder.theme
    }

    private fun RecyclerView.dividerDrawable() = ShapeDrawable(RectShape()).apply {
        intrinsicWidth = dp(1)
        intrinsicHeight = dp(1)
        paint.color = theme.dividerColor.color
    }

    fun RecyclerView.addGridDecoration() =
        addItemDecoration(GridDecoration(dividerDrawable()))

    fun RecyclerView.setupGridLayoutManager(
        adapter: GridCandidateViewAdapter,
        spanCount: Int = INITIAL_SPAN_COUNT,
        scrollVertically: Boolean = true
    ) {
        layoutManager =
            object : GridLayoutManager(context, spanCount, VERTICAL, false) {
                override fun canScrollVertically() = scrollVertically
                override fun setSpanCount(spanCount: Int) {
                    super.setSpanCount(spanCount)
                    (spanSizeLookup as? SpanHelper)?.invalidate()
                }
            }.apply {
                SpanHelper(adapter, this).attach()
            }
        this.adapter = adapter
    }

    fun RecyclerView.setupFlexboxLayoutManager(
        adapter: SimpleCandidateViewAdapter,
        scrollVertically: Boolean,
        init: (FlexboxLayoutManager.() -> Unit)? = null
    ) {
        layoutManager = object : FlexboxLayoutManager(context) {
            override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) =
                LayoutParams(lp).apply {
                    flexGrow = 1f
                }

            override fun canScrollVertically(): Boolean = scrollVertically
        }.apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.SPACE_AROUND
            alignItems = AlignItems.FLEX_START
            flexWrap = FlexWrap.WRAP
            init?.invoke(this)
        }
        this.adapter = adapter
    }

    fun RecyclerView.addFlexboxHorizontalDecoration() =
        addItemDecoration(FlexboxHorizontalDecoration(dividerDrawable()))

    fun RecyclerView.addFlexboxVerticalDecoration() =
        addItemDecoration(FlexboxVerticalDecoration(dividerDrawable()))

    companion object {
        private const val INITIAL_SPAN_COUNT = 6
    }
}