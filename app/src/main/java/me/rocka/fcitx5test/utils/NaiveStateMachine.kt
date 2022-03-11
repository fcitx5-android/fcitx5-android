package me.rocka.fcitx5test.utils

open class NaiveStateMachine<V : NaiveStateMachine.State, L>(
    initialState: V,
    // `state1` to `event` to `state2`
    vararg transitions: Pair<Pair<V, L>, V>
) {

    interface State

    protected val edges = transitions
        .map { Triple(it.first.first, it.second, it.first.second) }
        .distinct()

    protected val vertices = edges
        .flatMap { listOf(it.first, it.second) }
        .distinct()

    protected val labels = edges
        .map { it.third }
        .distinct()

    protected val graph = Array(vertices.size) {
        IntArray(vertices.size) { -1 }
    }.apply {
        edges.forEach { (v1, v2, l) ->
            this[vertices.indexOf(v1)][vertices.indexOf(v2)] = labels.indexOf(l)
        }
    }

    protected var currentStateIx = vertices.indexOf(initialState)

    var onNewStateListener: ((V) -> Unit)? = null

    val currentState
        get() = vertices[currentStateIx]

    protected fun edgesOfCurrentState() =
        graph[currentStateIx]
            .asIterable()
            .mapIndexedNotNull { v2Idx, labelIdx ->
                labelIdx.takeIf { it != -1 }?.run {
                    (v2Idx to vertices[v2Idx]) to (labelIdx to labels[labelIdx])
                }
            }

    /**
     * Single step transition
     */
    fun transitTo(state: V, selector: (L) -> Boolean) {
        if (state !in vertices)
            throw IllegalArgumentException("$state is an unknown state")
        if (currentState == state)
            return
        val transitions = edgesOfCurrentState()
        val filtered = transitions.filter { selector(it.second.second) }
        when (filtered.size) {
            0 -> throw IllegalStateException("Can not find any transition $currentState -> $state")
            1 -> {
                currentStateIx = filtered.first().first.first
                onNewStateListener?.invoke(state)
            }
            else -> throw IllegalStateException("More than one transitions are found $currentState -> $state")
        }
    }


}

fun <V : NaiveStateMachine.State> NaiveStateMachine<V, () -> Boolean>.transitTo(state: V) =
    transitTo(state) { it() }

class EventStateMachine<V : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
    initialState: V,
    vararg transitions: Pair<Pair<V, Event>, V>
) : NaiveStateMachine<V, Event>(initialState, *transitions) {

    interface StateTransitionEvent

    /**
     * Post an event that may trigger transition of state
     *
     * @return if transition happens
     */
    fun post(event: Event): Boolean {
        if (event !in labels)
            throw IllegalArgumentException("$event is an unknown event")
        val transitions = edgesOfCurrentState()
        val filtered = transitions.filter { it.second.second == event }
        return when (filtered.size) {
            0 -> {
                false
            }
            1 -> {
                currentStateIx = filtered.first().first.first
                onNewStateListener?.invoke(filtered.first().first.second)
                true
            }
            else -> throw IllegalStateException("More than one transitions are found given $event on $currentState")
        }
    }
}

// DSL

infix fun <State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent> State.on(
    event: Event
) =
    EventTransitionBuilder(this, event)

infix fun <State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent>
        EventTransitionBuilder<State, Event>.transitTo(
    endState: State
) = run {
    this.endState = endState
    build()
}

class EventTransitionBuilder<State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
    private val startState: State,
    private val event: Event
) {
    lateinit var endState: State
    fun build() = startState to event to endState

}