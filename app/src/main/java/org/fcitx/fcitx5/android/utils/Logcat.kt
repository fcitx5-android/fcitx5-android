package org.fcitx.fcitx5.android.utils

import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Logcat : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private var process: java.lang.Process? = null
    private var emittingJob: Job? = null

    private val flow: MutableSharedFlow<String> = MutableSharedFlow()

    /**
     * Subscribe to this flow to receive log in app
     * Nothing would be emitted until [initLogFlow] was called
     */
    val logFlow: SharedFlow<String> by lazy { flow.asSharedFlow() }

    /**
     * Get a snapshot of logcat
     */
    fun getLogAsync(): Deferred<Result<List<String>>> = async {
        runCatching {
            Runtime.getRuntime()
                .exec(arrayOf("logcat", "--pid=${Process.myPid()}", "-d"))
                .inputStream
                .bufferedReader()
                .readLines()
        }
    }

    /**
     * Clear logcat
     */
    fun clearLog(): Job =
        launch {
            runCatching { Runtime.getRuntime().exec(arrayOf("logcat", "-c")) }
        }

    /**
     * Create a process reading logcat, sending lines to [logFlow]
     */
    fun initLogFlow() =
        if (process != null)
            throw IllegalStateException("Logcat process already created!")
        else launch {
            runCatching {
                Runtime
                    .getRuntime()
                    .exec(arrayOf("logcat", "--pid=${Process.myPid()}", "-v", "brief"))
                    .also { process = it }
                    .inputStream
                    .bufferedReader()
                    .lineSequence()
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .cancellable()
                    .collect { flow.emit(it) }
            }
        }.also { emittingJob = it }

    /**
     * Destroy the reading process
     */
    fun shutdownLogFlow() {
        process?.destroy()
        emittingJob?.cancel()
    }
}
