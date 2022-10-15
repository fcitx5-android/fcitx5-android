package org.fcitx.fcitx5.android.core

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class FcitxDispatcher(private val controller: FcitxController) : CoroutineDispatcher() {

    class WrappedRunnable(private val runnable: Runnable, private val name: String? = null) :
        Runnable by runnable {
        private val time = System.currentTimeMillis()
        var started = false
            private set

        private val delta
            get() = System.currentTimeMillis() - time

        override fun run() {
            if (delta > JOB_WAITING_LIMIT)
                Timber.w("${toString()} has waited $delta ms to get run since created!")
            started = true
            runnable.run()
        }

        override fun toString(): String = "WrappedRunnable[${name ?: hashCode()}]"
    }

    // this is fcitx main thread
    private val internalDispatcher = Executors.newSingleThreadExecutor {
        Thread(it).apply {
            name = "fcitx-main"
        }
    }.asCoroutineDispatcher()

    private val internalScope = CoroutineScope(internalDispatcher)

    interface FcitxController {
        fun nativeStartup()
        fun nativeLoopOnce()
        fun nativeScheduleEmpty()
        fun nativeExit()
    }

    private val nativeLoopLock = Mutex()
    private val runningLock = Mutex()

    private val queue = ConcurrentLinkedQueue<WrappedRunnable>()

    private val isRunning = AtomicBoolean(false)

//    Disabled for now
//    private val aliveChecker = AliveChecker(ALIVE_CHECKER_PERIOD)

    fun start() {
        internalScope.launch {
            runningLock.withLock {
                Timber.i("Start")
                if (isRunning.compareAndSet(false, true)) {
                    Timber.d("Calling native startup")
                    controller.nativeStartup()
//                    aliveChecker.install()
                    while (isActive && isRunning.get()) {
                        // blocking...
                        nativeLoopLock.withLock {
                            controller.nativeLoopOnce()
                        }
                        // do scheduled jobs
                        while (true) {
                            val block = queue.poll() ?: break
                            block.run()
                        }
                    }
                    Timber.i("Calling native exit")
//                    aliveChecker.teardown()
                    controller.nativeExit()
                }
            }
        }
    }

    // blocking until stopped
    fun stop(): List<Runnable> {
        Timber.i("FcitxDispatcher stop()")
        return if (isRunning.compareAndSet(true, false)) {
            runBlocking {
                bypass()
                runningLock.withLock {
                    val rest = queue.toList()
                    queue.clear()
                    rest
                }
            }
        } else emptyList()
    }

    private fun dispatchInternal(block: Runnable, name: String? = null): WrappedRunnable {
        if (!isRunning.get())
            throw IllegalStateException("Dispatcher is not in running state!")
        val wrapped = WrappedRunnable(block, name)
        queue.offer(wrapped)
        bypass()
        return wrapped
    }

    // bypass nativeLoopOnce if no code is executing in native dispatcher
    private fun bypass() {
        if (nativeLoopLock.isLocked)
            controller.nativeScheduleEmpty()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        this@FcitxDispatcher.dispatchInternal(block)
    }

    companion object {
        const val JOB_WAITING_LIMIT = 2000L
//        const val ALIVE_CHECKER_PERIOD = 8000L
    }

}