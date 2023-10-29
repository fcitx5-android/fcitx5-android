/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FcitxLifecycleRegistry : FcitxLifecycle {

    private val observers = ConcurrentLinkedQueue<FcitxLifecycleObserver>()

    override fun addObserver(observer: FcitxLifecycleObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: FcitxLifecycleObserver) {
        observers.remove(observer)
    }

    override val currentState: FcitxLifecycle.State
        get() = internalState

    private var internalState = FcitxLifecycle.State.STOPPED

    override val lifecycleScope: CoroutineScope =
        FcitxLifecycleCoroutineScope(this).also { addObserver(it) }

    fun postEvent(event: FcitxLifecycle.Event) = synchronized(internalState) {
        when (event) {
            FcitxLifecycle.Event.ON_START -> {
                ensureAt(FcitxLifecycle.State.STOPPED)
                internalState = FcitxLifecycle.State.STARTING
            }
            FcitxLifecycle.Event.ON_READY -> {
                ensureAt(FcitxLifecycle.State.STARTING)
                internalState = FcitxLifecycle.State.READY
            }
            FcitxLifecycle.Event.ON_STOP -> {
                ensureAt(FcitxLifecycle.State.READY)
                internalState = FcitxLifecycle.State.STOPPING
            }
            FcitxLifecycle.Event.ON_STOPPED -> {
                ensureAt(FcitxLifecycle.State.STOPPING)
                internalState = FcitxLifecycle.State.STOPPED
            }
        }
        observers.forEach { it.onStateChanged(event) }
    }

    private fun ensureAt(state: FcitxLifecycle.State) = takeIf { (currentState == state) }
        ?: throw IllegalStateException("Currently not at $state!")

}

interface FcitxLifecycle {
    val currentState: State
    val lifecycleScope: CoroutineScope

    fun addObserver(observer: FcitxLifecycleObserver)
    fun removeObserver(observer: FcitxLifecycleObserver)

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

fun interface FcitxLifecycleObserver {
    fun onStateChanged(event: FcitxLifecycle.Event)
}

class FcitxLifecycleCoroutineScope(
    val lifecycle: FcitxLifecycle,
    override val coroutineContext: CoroutineContext = SupervisorJob()
) : CoroutineScope, FcitxLifecycleObserver {
    override fun onStateChanged(event: FcitxLifecycle.Event) {
        if (lifecycle.currentState >= FcitxLifecycle.State.STOPPING) {
            coroutineContext.cancelChildren()
        }
    }
}

suspend fun <T> FcitxLifecycle.whenAtState(
    state: FcitxLifecycle.State,
    block: suspend CoroutineScope.() -> T
): T =
    if (state == currentState) block(lifecycleScope)
    else AtStateHelper(this, state).run(block)

suspend inline fun <T> FcitxLifecycle.whenReady(noinline block: suspend CoroutineScope.() -> T) =
    whenAtState(FcitxLifecycle.State.READY, block)

suspend inline fun <T> FcitxLifecycle.whenStopped(noinline block: suspend CoroutineScope.() -> T) =
    whenAtState(FcitxLifecycle.State.STOPPED, block)

fun <T> FcitxLifecycle.launchWhenReady(block: suspend CoroutineScope.() -> T) =
    lifecycleScope.launch { whenReady(block) }

fun <T> FcitxLifecycle.launchWhenStopped(block: suspend CoroutineScope.() -> T) =
    lifecycleScope.launch { whenStopped(block) }

private class AtStateHelper(val lifecycle: FcitxLifecycle, val state: FcitxLifecycle.State) {
    private val observer = FcitxLifecycleObserver {
        if (lifecycle.currentState == state)
            continuation?.resume(Unit)
    }

    init {
        lifecycle.addObserver(observer)
    }

    private var continuation: Continuation<Unit>? = null

    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T {
        suspendCoroutine { continuation = it }
        lifecycle.removeObserver(observer)
        return block(lifecycle.lifecycleScope)
    }
}

