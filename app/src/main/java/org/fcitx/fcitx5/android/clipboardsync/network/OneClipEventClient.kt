package org.fcitx.fcitx5.android.clipboardsync.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class OneClipEventClient(serverUrl: String) {
    companion object {
        private const val ROOT_PATH = "/"
        private const val CURRENT_PATH = "/api/current"
        private const val EVENTS_PATH = "/api/events"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val baseUrl = normalizeServerUrl(serverUrl)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val connected = AtomicBoolean(false)
    private val closeRequested = AtomicBoolean(false)
    private val connectSignal = CompletableDeferred<Unit>()
    private val closeSignal = CompletableDeferred<Throwable?>()

    @Volatile
    private var activeCall: Call? = null

    suspend fun connect(onEvent: (OneClipEventData) -> Unit) {
        warmupSession()
        openEventStream(onEvent)
        connectSignal.await()
    }

    suspend fun awaitClose(): Throwable? = closeSignal.await()

    fun isConnected(): Boolean = connected.get()

    fun close() {
        closeRequested.set(true)
        connected.set(false)
        activeCall?.cancel()
        activeCall = null
        if (!closeSignal.isCompleted) {
            closeSignal.complete(null)
        }
    }

    private suspend fun warmupSession() = withContext(Dispatchers.IO) {
        request(ROOT_PATH)
        request(CURRENT_PATH)
    }

    private fun openEventStream(onEvent: (OneClipEventData) -> Unit) {
        val request = Request.Builder()
            .url(url(EVENTS_PATH))
            .get()
            .build()
        val call = client.newCall(request)
        activeCall = call

        thread(
            name = "OneClipEventStream",
            isDaemon = true
        ) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("OneClip SSE failed: ${response.code} ${response.message}")
                    }
                    connected.set(true)
                    if (!connectSignal.isCompleted) {
                        connectSignal.complete(Unit)
                    }

                    val source = response.body?.source()
                        ?: throw IOException("OneClip SSE body is empty")
                    val dataLines = mutableListOf<String>()

                    while (!closeRequested.get()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) {
                            dispatchEvent(dataLines, onEvent)
                            dataLines.clear()
                            continue
                        }
                        if (line.startsWith("data:")) {
                            dataLines += line.removePrefix("data:").trimStart()
                        }
                    }
                }

                if (!closeRequested.get()) {
                    fail(IOException("OneClip SSE disconnected"))
                } else {
                    finishGracefully()
                }
            } catch (error: Exception) {
                if (closeRequested.get() || isCanceled(error)) {
                    finishGracefully()
                } else {
                    fail(error)
                }
            }
        }
    }

    private fun dispatchEvent(
        dataLines: List<String>,
        onEvent: (OneClipEventData) -> Unit
    ) {
        if (dataLines.isEmpty()) return
        val payload = dataLines.joinToString("\n")
        val event = runCatching { json.decodeFromString<OneClipEventData>(payload) }
            .getOrElse { return }
        onEvent(event)
    }

    private fun request(path: String) {
        val request = Request.Builder()
            .url(url(path))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OneClip warmup failed for $path: ${response.code} ${response.message}")
            }
        }
    }

    private fun fail(error: Throwable) {
        connected.set(false)
        if (!connectSignal.isCompleted) {
            connectSignal.completeExceptionally(error)
        }
        if (!closeSignal.isCompleted) {
            closeSignal.complete(error)
        }
    }

    private fun finishGracefully() {
        connected.set(false)
        if (!connectSignal.isCompleted) {
            connectSignal.completeExceptionally(IOException("OneClip SSE closed before ready"))
        }
        if (!closeSignal.isCompleted) {
            closeSignal.complete(null)
        }
    }

    private fun url(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return baseUrl + normalizedPath
    }

    private fun normalizeServerUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    private fun isCanceled(error: Exception): Boolean {
        return error is IOException && error.message?.contains("canceled", ignoreCase = true) == true
    }
}
