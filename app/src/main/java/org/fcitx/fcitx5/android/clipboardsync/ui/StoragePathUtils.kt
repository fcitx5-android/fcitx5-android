package org.fcitx.fcitx5.android.clipboardsync.ui

import android.net.Uri
import android.provider.DocumentsContract
import java.net.URLDecoder

object StoragePathUtils {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY_STORAGE_PREFIX = "/storage/emulated/0"
    private const val UTF_8_NAME = "UTF-8"

    fun formatStoragePath(rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null

        val trimmed = rawPath.trim()
        if (!trimmed.startsWith("content://")) {
            return normalizeVisiblePath(URLDecoder.decode(trimmed, UTF_8_NAME))
        }

        val uri = Uri.parse(trimmed)
        val documentPath = runCatching {
            runCatching { DocumentsContract.getDocumentId(uri) }.getOrElse {
                DocumentsContract.getTreeDocumentId(uri)
            }
        }.getOrNull() ?: return normalizeVisiblePath(URLDecoder.decode(trimmed, UTF_8_NAME))

        val (volume, relativePath) = documentPath.split(':', limit = 2)
            .let { it.firstOrNull().orEmpty() to it.getOrElse(1) { "" } }

        val normalized = when {
            volume.equals("primary", ignoreCase = true) -> {
                buildString {
                    append(PRIMARY_STORAGE_PREFIX)
                    if (relativePath.isNotBlank()) {
                        append("/")
                        append(relativePath)
                    }
                }
            }

            volume.isNotBlank() -> {
                buildString {
                    append("/storage/")
                    append(volume)
                    if (relativePath.isNotBlank()) {
                        append("/")
                        append(relativePath)
                    }
                }
            }

            else -> URLDecoder.decode(trimmed, UTF_8_NAME)
        }

        return normalizeVisiblePath(normalized)
    }

    fun resolveDownloadUri(displayPath: String?, storedUri: String?): Uri? {
        storedUri?.trim()?.takeIf { it.isNotEmpty() }?.let { return Uri.parse(it) }

        val raw = displayPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.startsWith("content://") || raw.startsWith("file://")) {
            return Uri.parse(raw)
        }

        return buildTreeUriFromVisiblePath(raw)
    }

    fun derivePersistableUri(displayPath: String): String? {
        val trimmed = displayPath.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) return trimmed
        return buildTreeUriFromVisiblePath(trimmed)?.toString()
    }

    fun visiblePathFromUriString(uriString: String?): String? {
        return formatStoragePath(uriString)
    }

    private fun buildTreeUriFromVisiblePath(rawPath: String): Uri? {
        val normalized = ensureLeadingSlash(rawPath)

        val (volume, relativePath) = when {
            normalized == "/" -> "primary" to ""
            normalized == PRIMARY_STORAGE_PREFIX -> "primary" to ""
            normalized.startsWith("$PRIMARY_STORAGE_PREFIX/") -> {
                "primary" to normalized.removePrefix("$PRIMARY_STORAGE_PREFIX/").trim('/')
            }

            normalized.startsWith("/storage/") -> {
                val remaining = normalized.removePrefix("/storage/")
                val volume = remaining.substringBefore("/")
                val relative = remaining.substringAfter("/", "").trim('/')
                if (volume.isBlank()) return null
                volume to relative
            }

            else -> "primary" to normalized.removePrefix("/").trim('/')
        }

        val documentId = if (relativePath.isBlank()) "$volume:" else "$volume:$relativePath"
        return DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId)
    }

    private fun normalizeVisiblePath(path: String): String {
        val normalized = ensureLeadingSlash(path)
            .replace(Regex("/{2,}"), "/")

        return when {
            normalized == PRIMARY_STORAGE_PREFIX -> "/"
            normalized.startsWith("$PRIMARY_STORAGE_PREFIX/") -> {
                "/" + normalized.removePrefix("$PRIMARY_STORAGE_PREFIX/").trimStart('/')
            }

            else -> normalized
        }
    }

    private fun ensureLeadingSlash(path: String): String {
        val trimmed = path.trim()
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}
