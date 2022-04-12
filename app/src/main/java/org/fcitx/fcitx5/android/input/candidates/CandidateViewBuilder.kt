package org.fcitx.fcitx5.android.input.candidates

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.*
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.SimpleCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.SpanHelper
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxHorizontalDecoration
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.FlexboxVerticalDecoration
import org.fcitx.fcitx5.android.input.candidates.expanded.decoration.GridDecoration
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.resources.styledDrawable

class CandidateViewBuilder : UniqueComponent<CandidateViewBuilder>(), Dependent,
    ManagedHandler by managedHandler() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val fcitx: Fcitx by manager.fcitx()

    fun gridAdapter() = object : GridCandidateViewAdapter() {
        override fun onTouchDown(view: View) {
            view.hapticIfEnabled()
        }

        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }
    }

    fun simpleAdapter() = object : SimpleCandidateViewAdapter() {
        override fun onTouchDown(view: View) {
            view.hapticIfEnabled()
        }

        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }
    }
//
//    // setup a listener that sets the span count of gird layout according to recycler view's width
//    fun RecyclerView.autoSpanCount() {
//        oneShotGlobalLayoutListener {
//            (layoutManager as GridLayoutManager).apply {
//                // set columns according to the width of recycler view
//                // last item doesn't need padding, so we assume recycler view is wider
//                spanCount = (measuredWidth + dimenPxSize(R.dimen.candidate_padding)) /
//                        (dimenPxSize(R.dimen.candidate_min_width) + dimenPxSize(R.dimen.candidate_padding))
//            }
//        }
//    }

    fun RecyclerView.addGridDecoration() =
        GridDecoration(styledDrawable(android.R.attr.listDivider)!!).also {
            addItemDecoration(it)
        }

    fun RecyclerView.setupGridLayoutManager(
        adapter: GridCandidateViewAdapter,
        scrollVertically: Boolean
    ) {
        layoutManager =
            object : GridLayoutManager(context, INITIAL_SPAN_COUNT, VERTICAL, false) {
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
            // there is a bug in measuring if the recycler view is used with linear layout with weight
            override fun isAutoMeasureEnabled(): Boolean = false
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
        FlexboxHorizontalDecoration(styledDrawable(android.R.attr.listDivider)!!).also {
            addItemDecoration(it)
        }

    fun RecyclerView.addFlexboxVerticalDecoration() =
        FlexboxVerticalDecoration(styledDrawable(android.R.attr.listDivider)!!).also {
            addItemDecoration(it)
        }

    companion object {
        private const val INITIAL_SPAN_COUNT = 6
    }
}