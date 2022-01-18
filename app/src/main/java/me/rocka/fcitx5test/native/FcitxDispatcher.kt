package me.rocka.fcitx5test.native

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class FcitxDispatcher(private val controller: FcitxController) : Runnable, CoroutineDispatcher() {

    interface FcitxController {
        fun nativeStartup()
        fun nativeLoopOnce()
        fun nativeScheduleEmpty()
        fun nativeExit()
    }

    // not concurrent
    object Watchdog {
        private var installed: Job? = null
        private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        fun install() {
            if (installed != null)
                throw IllegalStateException("Watchdog has been installed!")
            installed = GlobalScope.launch(dispatcher) {
                delay(SINGLE_JOB_TIME_LIMIT)
                throw RuntimeException("Fcitx main thread had been blocked for $SINGLE_JOB_TIME_LIMIT ms!")
            }
        }

        fun teardown() {
            (installed ?: throw IllegalStateException("Watchdog has not been installed!")).cancel()
            installed = null
        }

        inline fun withWatchdog(block: () -> Unit) {
            install()
            block()
            teardown()
        }

    }

    private val queue = ConcurrentLinkedQueue<Runnable>()

    private val _isRunning = AtomicBoolean(false)

    val isRunning: Boolean
        get() = _isRunning.get()

    private var nativeLooping = AtomicBoolean(false)

    override fun run() {
        Log.i(javaClass.name, "start running")
        if (_isRunning.compareAndSet(false, true)) {
            Log.i(javaClass.name, "calling native startup")
            controller.nativeStartup()
        }
        while (isRunning) {
            nativeLooping.set(true)
            // blocking...
            measureTimeMillis {
                controller.nativeLoopOnce()
            }.let { Log.d(javaClass.name, "native loop done, took $it ms") }
            nativeLooping.set(false)
            // do rest jobs
            measureTimeMillis {
                while (queue.peek() != null) {
                    val block = queue.poll()!!
                    Watchdog.withWatchdog {
                        block.run()
                    }
                }
            }.let { Log.d(javaClass.name, "jobs done, took $it ms") }
        }
        Log.i(javaClass.name, "calling native exit")
        controller.nativeExit()
    }

    fun stop(): List<Runnable> =
        if (_isRunning.compareAndSet(true, false)) {
            val rest = queue.toList()
            queue.clear()
            rest
        } else emptyList()

    fun dispatch(block: Runnable) {
        queue.offer(block)
        // notify native
        if (nativeLooping.get())
            controller.nativeScheduleEmpty()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch(block)
    }

    companion object {
        const val SINGLE_JOB_TIME_LIMIT = 5000L
    }

}