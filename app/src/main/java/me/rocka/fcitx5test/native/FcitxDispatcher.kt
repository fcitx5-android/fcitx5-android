package me.rocka.fcitx5test.native

import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class FcitxDispatcher(private val controller: FcitxController) : CoroutineScope {

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
            name = "FcitxMain"
        }
    }.asCoroutineDispatcher()

    interface FcitxController {
        fun nativeStartup()
        fun nativeLoopOnce()
        fun nativeScheduleEmpty()
        fun nativeExit()
    }

    // not concurrent
    inner class AliveChecker(private val period: Long) : CoroutineScope {
        private var installed: Job? = null
        private val dispatcher = Executors.newSingleThreadExecutor {
            Thread(it).apply {
                name = "AliveChecker"
            }
        }.asCoroutineDispatcher()

        fun install() {
            if (installed != null)
                throw IllegalStateException("AliveChecker has been installed!")
            installed = launch {
                // delay at first
                delay(10000L)
                while (isActive) {
                    val emptyRunnable = dispatchInternal(Runnable { })
                    delay(period)
                    if (!emptyRunnable.started)
                        throw RuntimeException("Alive checking failed! The job didn't get run after $period ms!")
                }
            }
        }

        fun teardown() {
            (installed
                ?: throw IllegalStateException("AliveChecker has not been installed!")).cancel()
            installed = null
        }


        override val coroutineContext: CoroutineContext = dispatcher
    }

    private val nativeLoopLock = ReentrantLock()
    private val exitingLock = ReentrantLock()
    private val exitingCondition = exitingLock.newCondition()

    private val queue = ConcurrentLinkedQueue<WrappedRunnable>()

    private val isRunning = AtomicBoolean(false)

    private val aliveChecker = AliveChecker(ALIVE_CHECKER_PERIOD)

    fun start() = launch {
        Timber.i("Start")
        if (isRunning.compareAndSet(false, true)) {
            Timber.d("Calling native startup")
            controller.nativeStartup()
            aliveChecker.install()
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
            exitingLock.withLock {
                Timber.i("Calling native exit")
                aliveChecker.teardown()
                controller.nativeExit()
                exitingCondition.signal()
            }
        }
    }


    // blocking until stopped
    fun stop(): List<Runnable> {
        Timber.i("Stop")
        return if (isRunning.compareAndSet(true, false)) {
            exitingLock.withLock {
                bypass()
                exitingCondition.await()
                val rest = queue.toList()
                queue.clear()
                rest
            }
        } else emptyList()
    }

    private fun dispatchInternal(block: Runnable, name: String? = null): WrappedRunnable {
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

    suspend fun <T> dispatch(block: () -> T): T = suspendCancellableCoroutine {
        dispatchInternal(Runnable {
            it.resume(block())
        })
    }

    override val coroutineContext: CoroutineContext = internalDispatcher

    companion object {
        const val JOB_WAITING_LIMIT = 2000L
        const val JOBS_TIME_LIMIT = 5000L
        const val ALIVE_CHECKER_PERIOD = 8000L
    }

}