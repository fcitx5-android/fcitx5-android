package me.rocka.fcitx5test.keyboard.candidates

import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.*
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.Fcitx
import me.rocka.fcitx5test.keyboard.FcitxInputMethodService
import me.rocka.fcitx5test.keyboard.KeyboardManager
import me.rocka.fcitx5test.keyboard.candidates.adapter.GridCandidateViewAdapter
import me.rocka.fcitx5test.keyboard.candidates.adapter.SimpleCandidateViewAdapter
import me.rocka.fcitx5test.utils.dependency.fcitx
import me.rocka.fcitx5test.utils.dependency.service
import me.rocka.fcitx5test.utils.oneShotGlobalLayoutListener
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import splitties.resources.dimenPxSize

class CandidateViewBuilder : UniqueComponent<CandidateViewBuilder>(), Dependent,
    ManagedHandler by managedHandler() {

    private val service: FcitxInputMethodService by manager.service()
    private val fcitx: Fcitx by manager.fcitx()
    private val keyboardManager: KeyboardManager by manager.must()

    fun gridAdapter() = object : GridCandidateViewAdapter() {
        override fun onTouchDown() = keyboardManager.currentKeyboard.haptic()
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }
    }

    fun simpleAdapter() = object : SimpleCandidateViewAdapter() {
        override fun onTouchDown() = keyboardManager.currentKeyboard.haptic()
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }
    }

    // setup a listener that sets the span count of gird layout according to recycler view's width
    fun RecyclerView.autoSpanCount() {
        oneShotGlobalLayoutListener {
            (layoutManager as GridLayoutManager).apply {
                // set columns according to the width of recycler view
                // last item doesn't need padding, so we assume recycler view is wider
                spanCount = (measuredWidth + dimenPxSize(R.dimen.candidate_padding)) /
                        (dimenPxSize(R.dimen.candidate_min_width) + dimenPxSize(R.dimen.candidate_padding))
                requestLayout()
            }
        }
    }

    fun RecyclerView.addGridDecoration() = GridDecoration(
        ResourcesCompat.getDrawable(
            resources,
            R.drawable.candidate_grid_divider,
            context.theme
        )!!
    ).also { addItemDecoration(it) }

    fun RecyclerView.setupGridLayoutManager(
        adapter: GridCandidateViewAdapter,
        scrollVertically: Boolean
    ) {
        layoutManager =
            object : GridLayoutManager(
                context, INITIAL_SPAN_COUNT, VERTICAL, false
            ) {
                override fun canScrollVertically(): Boolean {
                    return scrollVertically
                }
            }.apply {
                SpanHelper(adapter, this).attach()
            }
        this.adapter = adapter
    }

    fun RecyclerView.setupFlexboxLayoutManager(
        adapter: SimpleCandidateViewAdapter,
        scrollVertically: Boolean
    ) {
        layoutManager = object : FlexboxLayoutManager(context) {
            override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): RecyclerView.LayoutParams =
                LayoutParams(lp)

            override fun canScrollVertically(): Boolean = scrollVertically
        }.apply {
            flexDirection = FlexDirection.ROW

            justifyContent = JustifyContent.SPACE_AROUND
            alignItems = AlignItems.FLEX_START
            flexWrap = FlexWrap.WRAP
        }
        this.adapter = adapter
    }

    fun RecyclerView.addHorizontalDecoration() =
        DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        ).also { addItemDecoration(it) }

    companion object {
        private const val INITIAL_SPAN_COUNT = 6
    }
}