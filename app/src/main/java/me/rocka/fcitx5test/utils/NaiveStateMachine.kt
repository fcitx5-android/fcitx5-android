package me.rocka.fcitx5test.utils

open class NaiveStateMachine<V : NaiveStateMachine.State, L>(
    initialState: V,
    protected val edges: List<Edge<V, L>>
) {

    interface State

    data class Edge<V, L>(val vertex1: V, val vertex2: V, val label: L)

    protected val vertices = edges
        .flatMap { listOf(it.vertex1, it.vertex2) }
        .distinct()

    protected val labels = edges
        .map { it.label }
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

class EventStateMachine<State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
    initialState: State,
    edges: List<Edge<State, List<Event>>>
) : NaiveStateMachine<State, List<Event>>(initialState, edges) {

    interface StateTransitionEvent

    /**
     * Push an event that may trigger a transition of state
     */
    fun push(event: Event) {
        if (event !in labels.flatten())
            throw IllegalArgumentException("$event is an unknown event")
        val transitions = edgesOfCurrentState()
        val filtered = transitions.filter { event in it.second.second }
        when (filtered.size) {
            0 -> {
                // do nothing
            }
            1 -> {
                currentStateIx = filtered.first().first.first
                onNewStateListener?.invoke(filtered.first().first.second)
            }
            else -> throw IllegalStateException("More than one transitions are found given $event on $currentState")
        }
    }
}

// DSL

fun <State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent> eventStateMachine(
    initialState: State,
    builder: EventStateMachineBuilder<State, Event>.() -> Unit
) =
    EventStateMachineBuilder<State, Event>(initialState).apply(builder).build()

class EventStateMachineBuilder<State : NaiveStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
    private val initialState: State
) {
    private val map = mutableMapOf<Pair<State, State>, MutableList<Event>>()

    private var listener: ((State) -> Unit)? = null

    inner class EventTransitionBuilder(val startState: State) {
        lateinit var endState: State
        lateinit var event: Event

    }

    infix fun EventTransitionBuilder.transitTo(state: State) = this.apply {
        endState = state
    }

    infix fun EventTransitionBuilder.on(event: Event) = this.run {
        this.event = event
        if (startState to endState !in map)
            map[startState to endState] = mutableListOf(event)
        else
            map.getValue(startState to endState) += event
    }

    fun from(state: State) = EventTransitionBuilder(state)

    fun onNewState(block: (State) -> Unit) {
        listener = block
    }


    fun build() = EventStateMachine(
        initialState,
        map.map { (k, v) -> NaiveStateMachine.Edge(k.first, k.second, v) }).apply {
        listener?.let { onNewStateListener = it }
    }
}
