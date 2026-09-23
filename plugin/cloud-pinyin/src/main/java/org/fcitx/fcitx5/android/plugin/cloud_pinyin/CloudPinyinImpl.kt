/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.plugin.cloud_pinyin

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class CloudPinyinImpl {
    private fun parseProxy(str: String?): Proxy {
        if (str.isNullOrBlank()) return Proxy.NO_PROXY
        val uri = Uri.parse(str)
        val type = when (uri.scheme) {
            "socks", "socks5", "socks4" -> Proxy.Type.SOCKS
            "http", "https" -> Proxy.Type.HTTP
            else -> return Proxy.NO_PROXY
        }
        val host = uri.host
        val port = uri.port
        if (host.isNullOrBlank() || port <= 0) {
            return Proxy.NO_PROXY
        }
        return Proxy(type, InetSocketAddress.createUnresolved(host, port))
    }

    sealed interface Backend {
        fun buildURL(encoded: String): String
        fun parseBody(raw: String): String

        object Google : Backend {
            override fun buildURL(encoded: String) =
                "https://www.google.com/inputtools/request?ime=pinyin&text=$encoded"

            /**
[
    "SUCCESS",
    [
        [
            "hhh",
            [
                "哈哈哈"
            ],
            [
            ],
            {
                "annotation": ["ha ha ha"],
                "candidate_type": [0],
                "lc": ["16 16 16"]
            }
        ]
    ]
]
             */
            override fun parseBody(raw: String): String {
                val arr = JSONArray(raw)
                return try {
                    if (arr.getString(0) == "SUCCESS") {
                        arr.getJSONArray(1).getJSONArray(0).getJSONArray(1).getString(0)
                    } else {
                        ""
                    }
                } catch (_: Exception) {
                    ""
                }
            }
        }

        object GoogleCN : Backend {
            override fun buildURL(encoded: String) =
                "https://www.google.cn/inputtools/request?ime=pinyin&text=$encoded"

            override fun parseBody(raw: String) = Google.parseBody(raw)
        }

        object Baidu : Backend {
            override fun buildURL(encoded: String) =
                "https://olimenew.baidu.com/py?input=$encoded&inputtype=py&resultcoding=utf-8"

            /**
{
    "status":"T",
    "errno":"0",
    "errmsg":"",
    "result": [
        [
            ["哈哈哈",6,{"pinyin":"ha'ha'ha","type":"IMEDICT"}],
            ["好好好",9,{"pinyin":"hao'hao'hao","type":"IMEDICT"}],
            ["hhh",3,{"pinyin":"h'h'h","type":"IMEDICT"}],
            ["哼哼哼",12,{"pinyin":"heng'heng'heng","type":"IMEDICT"}]
        ]
    ]
}
             */
            override fun parseBody(raw: String): String {
                val obj = JSONObject(raw)
                return try {
                    if (obj.getString("status") == "T" && obj.getString("errno") == "0") {
                        obj.getJSONArray("result").getJSONArray(0).getJSONArray(0).getString(0)
                    } else {
                        ""
                    }
                } catch (_: Exception) {
                    ""
                }
            }
        }
    }

    private suspend fun httpGet(url: String, proxy: Proxy): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection(proxy) as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        val stream = if (conn.responseCode in 200..299) conn.getInputStream() else conn.errorStream
        stream.bufferedReader().use { it.readText() }
    }

    suspend fun request(payload: CloudPinyinPayload): String {
        val backend = when (payload.backend) {
            0 -> Backend.Google
            1 -> Backend.GoogleCN
            2 -> Backend.Baidu
            else -> Backend.GoogleCN
        }
        val proxy = parseProxy(payload.proxy)
        val url = backend.buildURL(Uri.encode(payload.pinyin))
        return backend.parseBody(httpGet(url, proxy))
    }
}
