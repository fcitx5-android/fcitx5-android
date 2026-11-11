package org.fcitx.fcitx5.android.clipboardsync.network

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ClipCascadeClient(
    serverUrl: String,
    private val username: String,
    private val password: String
) {
    companion object {
        private const val TAG = "FcitxClipboardSync"
        private const val LOGIN_PATH = "/login"
        private const val VALIDATE_SESSION_PATH = "/api/validate-session"
        private const val USER_INFO_PATH = "/api/user-info"
        private const val SOCKET_PATH = "/clipsocket"
        private const val SUBSCRIPTION_ID = "android-clipboard-sync"
        private const val SUBSCRIPTION_DESTINATION = "/user/queue/cliptext"
        private const val SEND_DESTINATION = "/app/cliptext"
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_NONCE_SIZE_BYTES = 16
        private const val GCM_TAG_SIZE_BYTES = 16
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val baseUrl = normalizeServerUrl(serverUrl)
    private val cookieJar = MemoryCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val connected = AtomicBoolean(false)
    private val closeRequested = AtomicBoolean(false)
    private val connectSignal = CompletableDeferred<Unit>()
    private val closeSignal = CompletableDeferred<Throwable?>()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var encryptionKey: ByteArray? = null

    suspend fun connect(onMessage: (ClipCascadeClipboardData) -> Unit) {
        login()
        validateSession()
        deriveEncryptionKey()
        openSocket(onMessage)
        connectSignal.await()
    }

    suspend fun sendClipboard(payload: String, type: String = "text", filename: String? = null) {
        val activeSocket = webSocket ?: throw IOException("ClipCascade websocket is not connected")
        if (!connected.get()) {
            throw IOException("ClipCascade websocket handshake is not complete")
        }

        val body = encodeOutgoingBody(
            ClipCascadeClipboardData(
                payload = payload,
                type = type,
                filename = filename
            )
        )
        val sent = activeSocket.send(
            encodeStompFrame(
                command = "SEND",
                headers = linkedMapOf("destination" to SEND_DESTINATION),
                body = body
            )
        )
        if (!sent) {
            throw IOException("Failed to queue ClipCascade message")
        }
    }

    fun close() {
        closeRequested.set(true)
        connected.set(false)
        webSocket?.send(encodeStompFrame("DISCONNECT"))
        webSocket?.close(1000, "disconnect")
        webSocket = null
        if (!closeSignal.isCompleted) {
            closeSignal.complete(null)
        }
    }

    suspend fun awaitClose(): Throwable? = closeSignal.await()

    fun isConnected(): Boolean = connected.get()

    suspend fun testConnection(): String {
        return try {
            connect { }
            "Connection Successful: ClipCascade session established"
        } finally {
            close()
        }
    }

    private suspend fun login() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$LOGIN_PATH")
            .post(
                FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .build()
            )
            .build()

        client.newCall(request).execute().use { response ->
            val location = response.header("Location").orEmpty()
            if (!response.isRedirect && !response.isSuccessful) {
                throw IOException("ClipCascade login failed: ${response.code} ${response.message}")
            }
            if (location.contains("error", ignoreCase = true)) {
                throw IOException("ClipCascade login failed: invalid credentials")
            }
        }
    }

    private suspend fun validateSession() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$VALIDATE_SESSION_PATH")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("ClipCascade session validation failed: ${response.code} ${response.message}")
            }
            val body = response.body?.string().orEmpty()
            val validation = json.decodeFromString<ClipCascadeSessionValidationResponse>(body)
            if (!validation.valid) {
                throw IOException("ClipCascade session is not valid")
            }
        }
    }

    private suspend fun deriveEncryptionKey() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$USER_INFO_PATH")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("ClipCascade user-info failed: ${response.code} ${response.message}")
            }
            val body = response.body?.string().orEmpty()
            val userInfo = json.decodeFromString<ClipCascadeUserInfoResponse>(body)
            encryptionKey = deriveKey(
                password = password,
                username = username,
                salt = userInfo.salt,
                rounds = userInfo.hashRounds
            )
        }
    }

    private fun openSocket(onMessage: (ClipCascadeClipboardData) -> Unit) {
        val request = Request.Builder()
            .url(webSocketUrl())
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectSent = webSocket.send(
                    encodeStompFrame(
                        command = "CONNECT",
                        headers = linkedMapOf(
                            "accept-version" to "1.1",
                            "host" to "localhost"
                        )
                    )
                )
                if (!connectSent) {
                    fail(IOException("Failed to send ClipCascade CONNECT frame"))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.isBlank()) return

                val frame = runCatching { parseStompFrame(text) }.getOrElse { error ->
                    Log.w(TAG, "ClipCascade invalid STOMP frame", error)
                    return
                }

                when (frame.command.uppercase(Locale.ROOT)) {
                    "CONNECTED" -> {
                        val subscribed = webSocket.send(
                            encodeStompFrame(
                                command = "SUBSCRIBE",
                                headers = linkedMapOf(
                                    "id" to SUBSCRIPTION_ID,
                                    "destination" to SUBSCRIPTION_DESTINATION
                                )
                            )
                        )
                        if (!subscribed) {
                            fail(IOException("Failed to subscribe to ClipCascade clipboard queue"))
                            return
                        }
                        connected.set(true)
                        if (!connectSignal.isCompleted) {
                            connectSignal.complete(Unit)
                        }
                    }

                    "MESSAGE" -> {
                        val clipboardData = runCatching {
                            decodeIncomingBody(frame.body)
                        }.getOrElse { error ->
                            Log.w(TAG, "ClipCascade payload decode failed", error)
                            return
                        }
                        onMessage(clipboardData)
                    }

                    "ERROR" -> {
                        fail(IOException("ClipCascade STOMP error: ${frame.headers["message"].orEmpty()}"))
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected.set(false)
                if (!connectSignal.isCompleted) {
                    connectSignal.completeExceptionally(
                        IOException("ClipCascade websocket closed before handshake: $code $reason")
                    )
                }
                finishClose(
                    if (closeRequested.get()) null else IOException("ClipCascade websocket closed: $code $reason")
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected.set(false)
                val error = IOException(
                    buildString {
                        append("ClipCascade websocket failure")
                        response?.let { append(": ${it.code} ${it.message}") }
                    },
                    t
                )
                fail(error)
            }
        })
    }

    private fun fail(error: Throwable) {
        connected.set(false)
        if (!connectSignal.isCompleted) {
            connectSignal.completeExceptionally(error)
        }
        finishClose(error)
        webSocket?.cancel()
        webSocket = null
    }

    private fun finishClose(error: Throwable?) {
        if (!closeSignal.isCompleted) {
            closeSignal.complete(error)
        }
    }

    private fun encodeOutgoingBody(data: ClipCascadeClipboardData): String {
        val plainBody = json.encodeToString(data)
        val key = encryptionKey ?: return plainBody
        val encryptedPayload = encrypt(key, plainBody.toByteArray(StandardCharsets.UTF_8))
        return json.encodeToString(encryptedPayload)
    }

    private fun decodeIncomingBody(body: String): ClipCascadeClipboardData {
        val encryptedPayload = runCatching {
            json.decodeFromString<ClipCascadeEncryptedPayload>(body)
        }.getOrNull()

        if (encryptedPayload != null &&
            encryptedPayload.nonce.isNotBlank() &&
            encryptedPayload.ciphertext.isNotBlank() &&
            encryptedPayload.tag.isNotBlank()
        ) {
            val key = encryptionKey ?: throw IOException("ClipCascade encrypted payload received without key")
            val plaintext = decrypt(key, encryptedPayload)
            return json.decodeFromString(String(plaintext, StandardCharsets.UTF_8))
        }

        return json.decodeFromString(body)
    }

    private fun deriveKey(password: String, username: String, salt: String, rounds: Int): ByteArray {
        val saltBytes = (username + password + salt).toByteArray(StandardCharsets.UTF_8)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, rounds, AES_KEY_SIZE_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun encrypt(key: ByteArray, plaintext: ByteArray): ClipCascadeEncryptedPayload {
        val nonce = ByteArray(GCM_NONCE_SIZE_BYTES)
        SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val sealed = cipher.doFinal(plaintext)
        val ciphertextSize = sealed.size - GCM_TAG_SIZE_BYTES
        val ciphertext = sealed.copyOfRange(0, ciphertextSize)
        val tag = sealed.copyOfRange(ciphertextSize, sealed.size)

        return ClipCascadeEncryptedPayload(
            nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            tag = Base64.encodeToString(tag, Base64.NO_WRAP)
        )
    }

    private fun decrypt(key: ByteArray, payload: ClipCascadeEncryptedPayload): ByteArray {
        val nonce = Base64.decode(payload.nonce, Base64.DEFAULT)
        val ciphertext = Base64.decode(payload.ciphertext, Base64.DEFAULT)
        val tag = Base64.decode(payload.tag, Base64.DEFAULT)
        val sealed = ByteArray(ciphertext.size + tag.size)
        System.arraycopy(ciphertext, 0, sealed, 0, ciphertext.size)
        System.arraycopy(tag, 0, sealed, ciphertext.size, tag.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(sealed)
    }

    private fun webSocketUrl(): String {
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid ClipCascade server url: $baseUrl")
        return httpUrl.newBuilder()
            .encodedPath(SOCKET_PATH)
            .build()
            .toString()
    }

    private fun normalizeServerUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) {
            throw IOException("ClipCascade server url is empty")
        }
        return trimmed
    }

    private fun encodeStompFrame(
        command: String,
        headers: LinkedHashMap<String, String> = linkedMapOf(),
        body: String = ""
    ): String {
        val builder = StringBuilder()
        builder.append(command).append('\n')
        headers.forEach { (key, value) ->
            builder.append(key).append(':').append(value).append('\n')
        }
        builder.append('\n')
        builder.append(body)
        builder.append('\u0000')
        return builder.toString()
    }

    private fun parseStompFrame(raw: String): StompFrame {
        val normalized = raw.trimEnd('\u0000')
        val parts = normalized.split("\n\n", limit = 2)
        val headerLines = parts.firstOrNull()
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (headerLines.isEmpty()) {
            throw IOException("Missing STOMP command")
        }

        val headers = linkedMapOf<String, String>()
        headerLines.drop(1).forEach { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@forEach
            headers[line.substring(0, separator)] = line.substring(separator + 1)
        }
        return StompFrame(
            command = headerLines.first().trim(),
            headers = headers,
            body = parts.getOrElse(1) { "" }
        )
    }

    private data class StompFrame(
        val command: String,
        val headers: Map<String, String>,
        val body: String
    )

    private class MemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host].orEmpty()
                .filter { cookie -> cookie.matches(url) }
        }
    }
}
