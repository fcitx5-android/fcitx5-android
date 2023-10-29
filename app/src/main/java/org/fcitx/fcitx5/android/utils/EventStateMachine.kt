/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import timber.log.Timber

class EventStateMachine<State : Any, Event : EventStateMachine.TransitionEvent<State, B>, B : EventStateMachine.BooleanStateKey>(
    private val initialState: State,
    private val externalBooleanStates: MutableMap<B, Boolean> = mutableMapOf()
) {

    interface BooleanStateKey {
        val name: String
    }

    interface TransitionEvent<State : Any, B : BooleanStateKey> {
        /**
         * INVARIANT: No side effects
         * @return the next state
         */
        fun accept(initialState: State, currentState: State, useBoolean: (B) -> Boolean?): State

    }

    var onNewStateListener: ((State) -> Unit)? = null

    var currentState = initialState
        private set

    private val enableDebugLog: Boolean by AppPrefs.getInstance().internal.verboseLog

    /**
     * Push an event that may trigger a transition of state
     */
    fun push(event: Event) {
        val newState = event.accept(initialState, currentState) { externalBooleanStates[it] }
        if (newState == currentState) {
            if (enableDebugLog)
                Timber.d("At $currentState, $event didn't change the state")
            return
        }
        if (enableDebugLog)
            Timber.d("At $currentState transited to $newState by $event")
        currentState = newState
        onNewStateListener?.invoke(newState)
    }

    /**
     * Update boolean states and push an event
     */
    fun push(event: Event, vararg booleanStates: Pair<B, Boolean>) {
        booleanStates.forEach {
            setBooleanState(it.first, it.second)
        }
        push(event)
    }

    fun setBooleanState(key: B, value: Boolean) {
        externalBooleanStates[key] = value
    }

    fun getBooleanState(key: B) =
        externalBooleanStates[key]

    fun unsafeJump(state: State) {
        currentState = state
        onNewStateListener?.invoke(state)
    }
}

// DSL
class TransitionEventBuilder<State : Any, B : EventStateMachine.BooleanStateKey> {

    private val enableDebugLog: Boolean by AppPrefs.getInstance().internal.verboseLog

    private var raw: ((State, State, (B) -> Boolean?) -> State)? = null

    inner class Builder(val source: State) {
        lateinit var target: State
        var pred: ((B) -> Boolean?) -> Boolean = { _ -> true }
    }

    infix fun Builder.transitTo(state: State) = apply {
        target = state
    }

    infix fun Builder.on(expected: Pair<B, Boolean>) = apply {
        this.pred = { it(expected.first) == expected.second }
    }

    infix fun Builder.onF(pred: ((B) -> Boolean?) -> Boolean) = apply {
        this.pred = pred
    }

    val builders = mutableListOf<Builder>()

    fun from(state: State) = Builder(state).also { builders += it }

    /**
     * Use either [from] or [accept] to build the transition event
     */
    fun accept(block: ((State, State, (B) -> Boolean?) -> State)) = apply {
        raw = block
    }


    fun build() =
        object : EventStateMachine.TransitionEvent<State, B> {
            override fun accept(
                initialState: State,
                currentState: State,
                useBoolean: (B) -> Boolean?
            ): State {
                if (raw != null)
                    return raw!!(initialState, currentState, useBoolean)
                val filtered = builders.filter { it.source == currentState && it.pred(useBoolean) }
                return when (filtered.size) {
                    0 -> currentState
                    1 -> filtered[0].target
                    else -> {
                        val first = filtered[0].target
                        if (enableDebugLog)
                            Timber.d("More than one target states at $currentState: ${filtered.joinToString()}. Take the first one: $first")
                        first
                    }
                }
            }
        }
}

typealias TransitionBuildBlock<State, B> = TransitionEventBuilder<State, B>.() -> Unit

class BuildTransitionEvent<State : Any, B : EventStateMachine.BooleanStateKey>(block: TransitionBuildBlock<State, B>) :
    EventStateMachine.TransitionEvent<State, B> {
    private val delegate: EventStateMachine.TransitionEvent<State, B> by lazy {
        TransitionEventBuilder<State, B>().also(block).build()
    }

    override fun accept(
        initialState: State,
        currentState: State,
        useBoolean: (B) -> Boolean?
    ): State =
        delegate.accept(initialState, currentState, useBoolean)

}
