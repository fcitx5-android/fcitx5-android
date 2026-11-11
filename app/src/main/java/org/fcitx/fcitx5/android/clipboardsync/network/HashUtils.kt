package org.fcitx.fcitx5.android.clipboardsync.network

import java.security.MessageDigest

object HashUtils {

    fun sha256(text: String): String =
        sha256(text.toByteArray(Charsets.UTF_8))

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun calculateFileHash(fileName: String, bytes: ByteArray): String =
        sha256(fileName.toByteArray(Charsets.UTF_8) + byteArrayOf(0) + bytes)
}
