package me.rocka.fcitx5test.input.candidates

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.input.candidates.adapter.BaseCandidateViewAdapter
import me.rocka.fcitx5test.input.candidates.adapter.GridCandidateViewAdapter
import me.rocka.fcitx5test.input.candidates.adapter.SimpleCandidateViewAdapter
import me.rocka.fcitx5test.utils.dependency.UniqueViewComponent
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.onDataChanged
import org.mechdancer.dependency.manager.must
import kotlin.properties.Delegates


class ExpandableCandidateComponent(private val onDataChange: (ExpandableCandidateComponent.() -> Unit)) :
    UniqueViewComponent<ExpandableCandidateComponent, ExpandableCandidateLayout>() {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()

    private val onStyleChange = Prefs.OnChangeListener<Style> { init() }

    val style: Style by Prefs.getInstance().expandableCandidateStyle.also {
        it.registerOnChangeListener(onStyleChange)
    }

    enum class State {
        Expanded,
        Shrunk
    }

    enum class Style {
        Grid,
        Flexbox;

        companion object : Prefs.StringLikeCodec<Style> {
            override fun decode(raw: String): Style? =
                runCatching { valueOf(raw) }.getOrNull()
        }
    }

    var state: State by Delegates.observable(State.Shrunk) { _, old, new ->
        if (old != new)
            onStateUpdate?.invoke(new)
    }
        private set

    var onStateUpdate: ((State) -> Unit)? = null

    private val parentConstraintLayout
        get() = view.parent as ConstraintLayout

    lateinit var adapter: BaseCandidateViewAdapter

    private var decoration: RecyclerView.ItemDecoration? = null

    fun init() {
        adapter = when (style) {
            Style.Grid -> builder.gridAdapter()
            Style.Flexbox -> builder.simpleAdapter()
        }
        adapter.onDataChanged {
            onDataChange()
        }
        view.recyclerView.apply {
            decoration?.let { removeItemDecoration(it) }
            with(builder) {
                when (style) {
                    Style.Grid -> {
                        autoSpanCount()
                        setupGridLayoutManager(
                            this@ExpandableCandidateComponent.adapter as GridCandidateViewAdapter,
                            true
                        )
                        decoration = addGridDecoration()
                    }
                    Style.Flexbox -> {
                        setupFlexboxLayoutManager(
                            this@ExpandableCandidateComponent.adapter as SimpleCandidateViewAdapter,
                            true
                        )
                        decoration = addFlexboxHorizontalDecoration()
                    }
                }

            }
        }
        state = State.Shrunk
    }

    override val view by lazy {
        ExpandableCandidateLayout(context)
    }

    private val shrunkConstraintSet by lazy {
        ConstraintSet()
            .apply {
                clone(parentConstraintLayout)
                clear(R.id.expanded_candidate_view, ConstraintSet.BOTTOM)
            }
    }

    private val expandedConstraintSet by lazy {
        ConstraintSet()
            .apply {
                clone(parentConstraintLayout)
                connect(
                    R.id.expanded_candidate_view,
                    ConstraintSet.BOTTOM,
                    ConstraintLayout.LayoutParams.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            }
    }

    private fun prepareAnimation() {
        val transition = ChangeBounds()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(parentConstraintLayout, transition)
    }

    fun expand(animation: Boolean = true) {
        if (state != State.Shrunk)
            return
        if (animation)
            prepareAnimation()
        expandedConstraintSet.applyTo(parentConstraintLayout)
        view.resetPosition()
        state = State.Expanded
    }

    fun shrink(animation: Boolean = true) {
        if (state != State.Expanded)
            return
        if (animation)
            prepareAnimation()
        shrunkConstraintSet.applyTo(parentConstraintLayout)
        view.resetPosition()
        state = State.Shrunk
    }
}

