/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class FcitxLifecycleRegistry : FcitxLifecycle {

    private val internalStateFlow = MutableStateFlow(FcitxLifecycle.State.STOPPED)

    override val stateFlow = internalStateFlow.asStateFlow()

    override val currentState: FcitxLifecycle.State
        get() = internalStateFlow.value

    private val job = SupervisorJob()

    override val lifecycleScope = CoroutineScope(job + Dispatchers.Default)

    fun postEvent(event: FcitxLifecycle.Event) {
        val newState = internalStateFlow.updateAndGet {
            when (event) {
                FcitxLifecycle.Event.ON_START -> {
                    ensureAt(it, FcitxLifecycle.State.STOPPED)
                    FcitxLifecycle.State.STARTING
                }
                FcitxLifecycle.Event.ON_READY -> {
                    ensureAt(it, FcitxLifecycle.State.STARTING)
                    FcitxLifecycle.State.READY
                }
                FcitxLifecycle.Event.ON_STOP -> {
                    ensureAt(it, FcitxLifecycle.State.READY)
                    FcitxLifecycle.State.STOPPING
                }
                FcitxLifecycle.Event.ON_STOPPED -> {
                    ensureAt(it, FcitxLifecycle.State.STOPPING)
                    FcitxLifecycle.State.STOPPED
                }
            }
        }
        if (newState >= FcitxLifecycle.State.STOPPING) {
            job.cancelChildren()
        }
    }

    private fun ensureAt(currentState: FcitxLifecycle.State, state: FcitxLifecycle.State) {
        if (currentState != state) {
            throw IllegalStateException("Currently not at $state!")
        }
    }
}

interface FcitxLifecycle {
    val stateFlow: StateFlow<State>
    val currentState: State
    val lifecycleScope: CoroutineScope

    enum class State {
        STARTING,
        READY,
        STOPPING,
        STOPPED
    }

    enum class Event {
        ON_START,
        ON_READY,
        ON_STOP,
        ON_STOPPED
    }
}

interface FcitxLifecycleOwner {
    val lifecycle: FcitxLifecycle
}

val FcitxLifecycleOwner.lifeCycleScope
    get() = lifecycle.lifecycleScope

suspend inline fun <T> FcitxLifecycle.whenAtState(
    state: FcitxLifecycle.State,
    block: suspend CoroutineScope.() -> T
): T {
    stateFlow.first { it == state }
    return block(lifecycleScope)
}

suspend inline fun <T> FcitxLifecycle.whenReady(noinline block: suspend CoroutineScope.() -> T) =
    whenAtState(FcitxLifecycle.State.READY, block)

suspend inline fun <T> FcitxLifecycle.whenStopped(noinline block: suspend CoroutineScope.() -> T) =
    whenAtState(FcitxLifecycle.State.STOPPED, block)

fun <T> FcitxLifecycle.launchWhenReady(block: suspend CoroutineScope.() -> T) =
    lifecycleScope.launch { whenReady(block) }

fun <T> FcitxLifecycle.launchWhenStopped(block: suspend CoroutineScope.() -> T) =
    lifecycleScope.launch { whenStopped(block) }
