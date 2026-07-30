/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.cloud_pinyin

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinCallback
import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinProvider
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class MainService : FcitxPluginService() {

    private lateinit var connection: FcitxRemoteConnection
    private var scope = newScope()
    private val requests = ConcurrentHashMap<Long, Job>()

    private val provider = object : ICloudPinyinProvider.Stub() {
        override fun request(
            requestId: Long,
            pinyin: String,
            backend: String,
            proxy: String,
            callback: ICloudPinyinCallback
        ) {
            requests.remove(requestId)?.cancel()
            requests[requestId] = scope.launch {
                val result = fetch(pinyin, backend, proxy)
                try {
                    callback.onResponse(requestId, result.status, result.body, result.error)
                } finally {
                    requests.remove(requestId)
                }
            }
        }
    }

    override fun start() {
        if (!scope.coroutineContext[Job]!!.isActive) {
            scope = newScope()
        }
        connection = bindFcitxRemoteService(BuildConfig.MAIN_APPLICATION_ID) {
            it.registerCloudPinyinProvider(provider)
        }
    }

    override fun stop() {
        requests.values.forEach { it.cancel() }
        requests.clear()
        scope.cancel()
        if (::connection.isInitialized) {
            runCatching {
                connection.remoteService?.unregisterCloudPinyinProvider(provider)
            }
            unbindService(connection)
        }
    }

    private fun fetch(pinyin: String, backend: String, proxyValue: String): Result {
        val url = endpoint(pinyin, backend) ?: return Result(0, ByteArray(0), "Unknown backend")
        Log.d(TAG, "Cloud pinyin request: $url")
        return try {
            val connection = (URL(url).openConnection(parseProxy(proxyValue)) as HttpURLConnection)
            connection.connectTimeout = REQUEST_TIMEOUT_MS
            connection.readTimeout = REQUEST_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.useCaches = false
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use(::readResponse) ?: ByteArray(0)
            if (status in 200..299) {
                Log.d(TAG, "Cloud pinyin response ($status): ${String(body, StandardCharsets.UTF_8)}")
            }
            connection.disconnect()
            Result(status, body, "")
        } catch (e: Exception) {
            Log.w(TAG, "Cloud pinyin request failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result(0, ByteArray(0), e.message ?: e.javaClass.simpleName)
        }
    }

    private fun endpoint(pinyin: String, backend: String): String? {
        val encoded = java.net.URLEncoder.encode(pinyin, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return when (backend) {
            "Google" -> "https://www.google.com/inputtools/request?ime=pinyin&text=$encoded"
            "GoogleCN" -> "https://www.google.cn/inputtools/request?ime=pinyin&text=$encoded"
            "Baidu" -> "https://olimenew.baidu.com/py?input=$encoded&inputtype=py&resultcoding=utf-8"
            else -> null
        }
    }

    private fun parseProxy(value: String): Proxy {
        if (value.isBlank()) return Proxy.NO_PROXY
        val uri = URI(value)
        val host = requireNotNull(uri.host) { "Proxy host is missing" }
        require(uri.port in 1..65535) { "Proxy port is invalid" }
        val type = when (uri.scheme.lowercase()) {
            "http", "https" -> Proxy.Type.HTTP
            "socks", "socks4", "socks5" -> Proxy.Type.SOCKS
            else -> error("Unsupported proxy scheme")
        }
        return Proxy(type, java.net.InetSocketAddress(host, uri.port))
    }

    private fun readResponse(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(512)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_RESPONSE_SIZE) { "Response exceeds 2048 bytes" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class Result(val status: Int, val body: ByteArray, val error: String)

    private companion object {
        const val TAG = "CloudPinyinService"
        const val REQUEST_TIMEOUT_MS = 10_000
        const val MAX_RESPONSE_SIZE = 2048
    }
}
