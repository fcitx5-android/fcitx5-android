package org.fcitx.fcitx5.android.utils.concurrent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class MVar<T : Any> {
    private val core = Channel<T>(1)

    val isEmpty
        get() = core.isEmpty

    suspend fun put(data: T) =
        core.send(data)

    suspend fun read(): T {
        val data = take()
        core.send(data)
        return data
    }

    suspend fun take(): T = core.receive()

    fun tryPut(data: T): Boolean = core.trySend(data).isSuccess

    fun tryTake(): T? = core.tryReceive().getOrNull()
}

suspend inline fun <T : Any, U> MVar<T>.with(block: (T) -> U): U {
    val data = read()
    return block(data)
}
