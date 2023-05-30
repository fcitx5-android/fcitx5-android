package org.fcitx.fcitx5.android.utils.concurrent

@JvmInline
value class Barrier<T : Any>(private val mVar: MVar<T>) {
    fun signal(data: T) {
        if (!mVar.tryPut(data))
            throw IllegalStateException("Barrier has already been signaled")
    }

    suspend fun wait() = mVar.read()

    fun reset() = mVar.tryTake()
}

fun <T : Any> barrier() = Barrier<T>(MVar())