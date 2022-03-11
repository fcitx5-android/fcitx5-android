package me.rocka.fcitx5test.utils

class EventStateMachine<State : EventStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
    initialState: State,
    private val stateGraph: ImmutableGraph<State, List<Event>>
) {

    interface State
    interface StateTransitionEvent

    private var currentStateIx = stateGraph.vertices.indexOf(initialState)

    var onNewStateListener: ((State) -> Unit)? = null

    val currentState
        get() = stateGraph.vertices[currentStateIx]

    /**
     * Push an event that may trigger a transition of state
     */
    fun push(event: Event) {
        if (event !in stateGraph.labels.flatten())
            throw IllegalArgumentException("$event is an unknown event")
        val transitions = stateGraph.getEdgesOfVertexWithIndex(currentState)
        val filtered = transitions.filter { event in it.second.label }
        when (filtered.size) {
            0 -> {
                // do nothing
            }
            1 -> {
                currentStateIx = filtered.first().first.first
                onNewStateListener?.invoke(filtered.first().second.vertex2)
            }
            else -> throw IllegalStateException("More than one transitions are found given $event on $currentState")
        }
    }
}

// DSL

fun <State : EventStateMachine.State, Event : EventStateMachine.StateTransitionEvent> eventStateMachine(
    initialState: State,
    builder: EventStateMachineBuilder<State, Event>.() -> Unit
) =
    EventStateMachineBuilder<State, Event>(initialState).apply(builder).build()

class EventStateMachineBuilder<State : EventStateMachine.State, Event : EventStateMachine.StateTransitionEvent>(
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
        ImmutableGraph(map.map { (k, v) ->
            ImmutableGraph.Edge(k.first, k.second, v)
        })
    ).apply {
        listener?.let { onNewStateListener = it }
    }
}
