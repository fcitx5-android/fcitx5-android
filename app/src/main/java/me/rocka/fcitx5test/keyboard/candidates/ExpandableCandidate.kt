package me.rocka.fcitx5test.keyboard.candidates

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.utils.dependency.context
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import kotlin.properties.Delegates


class ExpandableCandidate : UniqueComponent<ExpandableCandidate>(), Dependent,
    ManagedHandler by managedHandler() {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()

    enum class State {
        Expanded,
        Shrunk
    }

    var state: State by Delegates.observable(State.Shrunk) { _, _, new ->
        onStateUpdate?.invoke(new)
    }
        private set

    var onStateUpdate: ((State) -> Unit)? = null

    private val parentConstraintLayout
        get() = ui.root.parent as ConstraintLayout

    val adapter by lazy { builder.newCandidateViewAdapter() }
    val ui by lazy {
        ExpandedCandidateUi(context) {
            with(builder) {
                autoSpanCount()
                setupGridLayoutManager(this@ExpandableCandidate.adapter, true)
                addGridDecoration()
            }
        }
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
        ui.resetPosition()
        state = State.Expanded
    }

    fun shrink(animation: Boolean = true) {
        if (state != State.Expanded)
            return
        if (animation)
            prepareAnimation()
        shrunkConstraintSet.applyTo(parentConstraintLayout)
        ui.resetPosition()
        state = State.Shrunk
    }
}

