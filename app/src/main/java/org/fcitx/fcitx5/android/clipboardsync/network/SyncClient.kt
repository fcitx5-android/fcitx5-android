package org.fcitx.fcitx5.android.clipboardsync.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.TimeUnit

object SyncClient {
    class StaleClipboardContentException(message: String, cause: Throwable? = null) : IOException(message, cause)

    enum class ServerBackend {
        SYNCCLIPBOARD,
        ONECLIP,
        CLIPCASCADE;

        companion object {
            fun fromProfileType(profileType: String?): ServerBackend {
                return when {
                    profileType.equals("oneclip", ignoreCase = true) -> ONECLIP
                    profileType.equals("clipcascade", ignoreCase = true) -> CLIPCASCADE
                    else -> SYNCCLIPBOARD
                }
            }
        }
    }

    data class FetchResult(
        val items: List<ClipboardData> = emptyList(),
        val revision: String?
    )

    private const val TAG = "FcitxClipboardSync"
    private const val SYNCCLIPBOARD_HISTORY_PAGE_SIZE = 50
    private const val SAVED_URI_READY_RETRY_COUNT = 10
    private const val SAVED_URI_READY_RETRY_DELAY_MS = 150L
    private const val CLIPBOARD_CACHE_DIR = "clipboard_shared"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    private val ONECLIP_UNSUPPORTED_FILE_TYPES = setOf("code", "file")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val OCTET_STREAM_TYPE = "application/octet-stream".toMediaType()

    private fun fetchResult(data: ClipboardData?, revision: String?): FetchResult {
        return FetchResult(
            items = data?.let(::listOf).orEmpty(),
            revision = revision
        )
    }

    fun fetchClipboard(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        backend: ServerBackend,
        lastRevision: String? = null,
        downloadDirUri: Uri? = null,
        preDownloadFilter: ((ClipboardData) -> Boolean)? = null
    ): FetchResult {
        return when (backend) {
            ServerBackend.SYNCCLIPBOARD -> fetchSyncClipboard(
                context = context,
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                lastRevision = lastRevision,
                downloadDirUri = downloadDirUri,
                preDownloadFilter = preDownloadFilter
            )

            ServerBackend.ONECLIP -> fetchOneClipClipboard(
                context = context,
                serverUrl = serverUrl,
                lastRevision = lastRevision,
                downloadDirUri = downloadDirUri,
                preDownloadFilter = preDownloadFilter
            )

            ServerBackend.CLIPCASCADE -> {
                throw UnsupportedOperationException("ClipCascade uses a live websocket session instead of polling")
            }
        }
    }

    fun putClipboard(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        backend: ServerBackend,
        content: String
    ) {
        when (backend) {
            ServerBackend.SYNCCLIPBOARD -> putSyncClipboard(
                context = context,
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                content = content
            )

            ServerBackend.ONECLIP -> putOneClip(
                context = context,
                serverUrl = serverUrl,
                content = content
            )

            ServerBackend.CLIPCASCADE -> runBlocking {
                val client = ClipCascadeClient(
                    serverUrl = serverUrl,
                    username = username,
                    password = pass
                )
                try {
                    client.connect { }
                    val outgoing = buildClipCascadeClipboardData(context, content)
                    client.sendClipboard(
                        payload = outgoing.payload,
                        type = outgoing.type,
                        filename = outgoing.filename
                    )
                } finally {
                    client.close()
                }
            }
        }
    }

    fun testConnection(
        serverUrl: String,
        username: String,
        pass: String,
        backend: ServerBackend
    ): Result<String> {
        return try {
            val request = when (backend) {
                ServerBackend.SYNCCLIPBOARD -> {
                    val (_, jsonUrl) = getBaseAndJsonUrl(serverUrl)
                    Log.d(TAG, "[Test] Testing SyncClipboard connection to $jsonUrl")
                    Request.Builder()
                        .url(jsonUrl)
                        .header("Authorization", Credentials.basic(username, pass))
                        .get()
                        .build()
                }

                ServerBackend.ONECLIP -> {
                    val historyUrl = oneClipUrl(serverUrl, "/api/current")
                    Log.d(TAG, "[Test] Testing OneClip connection to $historyUrl")
                    Request.Builder()
                        .url(historyUrl)
                        .get()
                        .build()
                }

                ServerBackend.CLIPCASCADE -> {
                    return runBlocking {
                        runCatching {
                            ClipCascadeClient(serverUrl, username, pass).testConnection()
                        }
                    }
                }
            }

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connection Successful: ${response.code}")
                } else {
                    Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Test] Error", e)
            Result.failure(e)
        }
    }

    private fun fetchSyncClipboard(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        lastRevision: String?,
        downloadDirUri: Uri?,
        preDownloadFilter: ((ClipboardData) -> Boolean)?
    ): FetchResult {
        fetchSyncClipboardHistory(
            context = context,
            serverUrl = serverUrl,
            username = username,
            pass = pass,
            lastRevision = lastRevision,
            downloadDirUri = downloadDirUri,
            preDownloadFilter = preDownloadFilter
        )?.let { return it }

        return fetchSyncClipboardCurrent(
            context = context,
            serverUrl = serverUrl,
            username = username,
            pass = pass,
            lastRevision = lastRevision,
            downloadDirUri = downloadDirUri,
            preDownloadFilter = preDownloadFilter
        )
    }

    private fun fetchSyncClipboardCurrent(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        lastRevision: String?,
        downloadDirUri: Uri?,
        preDownloadFilter: ((ClipboardData) -> Boolean)?
    ): FetchResult {
        val previousEtag = lastRevision
            ?.takeIf { it.startsWith(REVISION_ETAG_PREFIX) }
            ?.removePrefix(REVISION_ETAG_PREFIX)

        val (partialData, newEtag) = fetchClipboardJson(serverUrl, username, pass, previousEtag)
        if (partialData == null) {
            val revision = newEtag?.let { REVISION_ETAG_PREFIX + it } ?: lastRevision
            return fetchResult(null, revision)
        }
        if (preDownloadFilter?.invoke(partialData) == false) {
            val revision = newEtag?.let { REVISION_ETAG_PREFIX + it } ?: buildRevisionToken(partialData)
            return fetchResult(null, revision)
        }

        val data = downloadDetails(context, serverUrl, username, pass, partialData, downloadDirUri)
        val revision = newEtag?.let { REVISION_ETAG_PREFIX + it } ?: buildRevisionToken(data)
        return fetchResult(data, revision)
    }

    private fun fetchSyncClipboardHistory(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        lastRevision: String?,
        downloadDirUri: Uri?,
        preDownloadFilter: ((ClipboardData) -> Boolean)?
    ): FetchResult? {
        val cursor = decodeSyncClipboardHistoryCursor(lastRevision)
        if (cursor == null) {
            val bootstrapPage = querySyncClipboardHistoryPage(
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                page = 1,
                modifiedAfter = null
            ) ?: return null

            val bootstrapCursor = buildBootstrapHistoryCursor(bootstrapPage)
            val bootstrapRevision = bootstrapCursor?.let(::encodeSyncClipboardHistoryCursor)

            val currentResult = fetchSyncClipboardCurrent(
                context = context,
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                lastRevision = null,
                downloadDirUri = downloadDirUri,
                preDownloadFilter = preDownloadFilter
            )

            return if (bootstrapRevision != null) {
                currentResult.copy(revision = bootstrapRevision)
            } else {
                currentResult
            }
        }

        val queriedRecords = mutableListOf<SyncClipboardHistoryRecord>()
        var page = 1
        while (true) {
            val pageRecords = querySyncClipboardHistoryPage(
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                page = page,
                modifiedAfter = cursor.modifiedAfter
            ) ?: return null
            if (pageRecords.isEmpty()) {
                break
            }
            queriedRecords += pageRecords
            if (pageRecords.size < SYNCCLIPBOARD_HISTORY_PAGE_SIZE) {
                break
            }
            page += 1
        }

        val sortedRecords = filterAndSortHistoryRecords(queriedRecords, cursor)
        if (sortedRecords.isEmpty()) {
            val historyRevision = encodeSyncClipboardHistoryCursor(cursor)

            // SyncClipboard can switch the current clipboard back to an existing
            // history entry without bumping that entry's LastModified timestamp.
            // When that happens the history API returns no delta, but
            // SyncClipboard.json still changes and must be reconciled.
            val currentResult = fetchSyncClipboardCurrent(
                context = context,
                serverUrl = serverUrl,
                username = username,
                pass = pass,
                lastRevision = null,
                downloadDirUri = downloadDirUri,
                preDownloadFilter = preDownloadFilter
            )
            return if (currentResult.items.isEmpty()) {
                FetchResult(emptyList(), historyRevision)
            } else {
                currentResult.copy(revision = historyRevision)
            }
        }

        val nextCursor = advanceSyncClipboardHistoryCursor(cursor, sortedRecords)
        val items = buildList {
            for (record in sortedRecords) {
                if (record.isDeleted) {
                    continue
                }
                val metadata = historyRecordToClipboardData(record)
                if (preDownloadFilter?.invoke(metadata) == false) {
                    continue
                }
                add(
                    if (record.hasData) {
                        downloadSyncClipboardHistoryData(
                            context = context,
                            serverUrl = serverUrl,
                            username = username,
                            pass = pass,
                            record = record,
                            metadata = metadata,
                            downloadDirUri = downloadDirUri
                        )
                    } else {
                        metadata
                    }
                )
            }
        }
        return FetchResult(items, encodeSyncClipboardHistoryCursor(nextCursor))
    }

    private fun fetchOneClipClipboard(
        context: Context,
        serverUrl: String,
        lastRevision: String?,
        downloadDirUri: Uri?,
        preDownloadFilter: ((ClipboardData) -> Boolean)?
    ): FetchResult {
        val currentRequest = Request.Builder()
            .url(oneClipUrl(serverUrl, "/api/current"))
            .get()
            .build()

        client.newCall(currentRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to load OneClip current item: ${response.code} ${response.message}")
            }

            val item = json.decodeFromString<ClipboardData>(response.body?.string().orEmpty())
            if (item.id.isBlank()) {
                return fetchResult(null, lastRevision)
            }
            val revision = buildOneClipRevision(item)

            if (revision == lastRevision) {
                return fetchResult(null, revision)
            }
            if (preDownloadFilter?.invoke(item) == false) {
                return fetchResult(null, revision)
            }

            val normalizedType = item.type.lowercase(Locale.ROOT)
            if (normalizedType == "image" || item.hasImage) {
                val imageData = downloadOneClipImage(
                    context = context,
                    serverUrl = serverUrl,
                    itemId = item.id,
                    timestamp = item.timestamp,
                    downloadDirUri = downloadDirUri
                )
                return fetchResult(imageData, revision)
            }
            if (normalizedType in ONECLIP_UNSUPPORTED_FILE_TYPES) {
                Log.d(TAG, "[Pull] Ignoring unsupported OneClip file-like item: type=$normalizedType id=${item.id}")
                return fetchResult(null, revision)
            }

            return fetchResult(
                item.copy(
                    type = "Text",
                    hash = item.id,
                    remoteTimestamp = (item.timestamp * 1000).toLong()
                ),
                revision
            )
        }
    }

    private fun putSyncClipboard(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        content: String
    ) {
        val (baseUrl, jsonUrl) = getBaseAndJsonUrl(serverUrl)
        val credential = Credentials.basic(username, pass)
        val uri = content.toClipboardUriOrNull()

        try {
            if (uri != null) {
                val fileName = getFileName(context, uri) ?: "unknown_file"
                val bytes = readClipboardUriBytes(context, uri)
                    ?: throw StaleClipboardContentException("Clipboard URI is no longer readable: $uri")

                Log.d(TAG, "[Push] Uploading file $fileName (${bytes.size} bytes)")
                val fileUrl = "${baseUrl}file/$fileName"
                val fileBody = bytes.toRequestBody(OCTET_STREAM_TYPE)
                val fileReq = Request.Builder()
                    .url(fileUrl)
                    .header("Authorization", credential)
                    .put(fileBody)
                    .build()

                client.newCall(fileReq).execute().use {
                    if (!it.isSuccessful) throw IOException("File upload failed: ${it.code}")
                }

                val data = ClipboardData(
                    type = "File",
                    text = fileName,
                    // SyncClipboard server rejects Android-side file hashes here.
                    // Leave it empty and let the server accept the uploaded file metadata.
                    hash = "",
                    hasData = true,
                    dataName = fileName,
                    size = bytes.size.toLong()
                )

                uploadSyncClipboardJson(jsonUrl, credential, data)
            } else {
                Log.d(TAG, "[Push] Uploading text (${content.length} chars)")
                val hash = HashUtils.sha256(content)
                val data = ClipboardData(
                    type = "Text",
                    text = content,
                    hash = hash,
                    hasData = false,
                    size = content.toByteArray().size.toLong()
                )
                uploadSyncClipboardJson(jsonUrl, credential, data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Push] Error", e)
            throw e
        }
    }

    private fun putOneClip(
        context: Context,
        serverUrl: String,
        content: String
    ) {
        val uri = content.toClipboardUriOrNull()
        try {
            primeOneClipSession(serverUrl)
            if (uri != null && isImageUri(context, uri)) {
                val bytes = readClipboardUriBytes(context, uri)
                    ?: throw StaleClipboardContentException("Clipboard image URI is no longer readable: $uri")
                uploadOneClipImage(serverUrl, bytes)
            } else if (uri != null && !isReadableClipboardUri(context, uri)) {
                throw StaleClipboardContentException("Clipboard URI is no longer readable: $uri")
            } else {
                uploadOneClipText(serverUrl, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Push] OneClip upload failed", e)
            throw e
        }
    }

    @Throws(IOException::class)
    private fun fetchClipboardJson(
        serverUrl: String,
        username: String,
        pass: String,
        etag: String? = null
    ): Pair<ClipboardData?, String?> {
        val (_, jsonUrl) = getBaseAndJsonUrl(serverUrl)
        val credential = Credentials.basic(username, pass)
        val requestBuilder = Request.Builder()
            .url(jsonUrl)
            .header("Authorization", credential)
            .get()

        if (!etag.isNullOrEmpty()) {
            requestBuilder.header("If-None-Match", etag)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 304) {
                return null to (response.header("ETag") ?: etag)
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "[Pull] Failed: ${response.code} ${response.message}")
                throw IOException("Unexpected code $response")
            }

            val newEtag = response.header("ETag")
            var bodyString = response.body?.string() ?: ""
            if (bodyString.startsWith("\uFEFF")) {
                bodyString = bodyString.substring(1)
            }
            return json.decodeFromString<ClipboardData>(bodyString) to newEtag
        }
    }

    @Throws(IOException::class)
    fun downloadDetails(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        data: ClipboardData,
        downloadDirUri: Uri? = null
    ): ClipboardData {
        if (!data.hasData || data.dataName.isEmpty()) {
            return data
        }

        val (baseUrl, _) = getBaseAndJsonUrl(serverUrl)
        val credential = Credentials.basic(username, pass)
        val fileUrl = "${baseUrl}file/${data.dataName}"

        val fileReq = Request.Builder()
            .url(fileUrl)
            .header("Authorization", credential)
            .get()
            .build()

        var newData = data

        client.newCall(fileReq).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")
            val bytes = response.body?.bytes() ?: ByteArray(0)

            if (data.hash.isNotEmpty()) {
                val calculatedHash = if (data.type.equals("Text", ignoreCase = true)) {
                    HashUtils.sha256(bytes)
                } else {
                    HashUtils.calculateFileHash(data.dataName, bytes)
                }

                if (!calculatedHash.equals(data.hash, ignoreCase = true)) {
                    Log.e(TAG, "[Pull] Hash mismatch! Expected: ${data.hash}, Got: $calculatedHash")
                }
            }

            if (data.type.equals("Text", ignoreCase = true)) {
                newData = data.copy(
                    text = String(bytes, Charsets.UTF_8),
                    mimeType = "text/plain"
                )
            } else {
                val mimeType = response.body?.contentType()?.toString().orEmpty()
                val savedUri = saveIncomingBytes(
                    context = context,
                    dirUri = downloadDirUri,
                    fileName = data.dataName,
                    bytes = bytes,
                    mimeType = mimeType.ifBlank { "*/*" }
                )
                if (savedUri != null) {
                    newData = data.copy(
                        text = savedUri.toString(),
                        mimeType = mimeType
                    )
                }
            }
        }

        return newData
    }

    private fun uploadSyncClipboardJson(url: String, credential: String, data: ClipboardData) {
        val jsonString = json.encodeToString(data)
        val body = jsonString.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("SyncClipboard JSON upload failed: ${response.code} ${response.message}")
            }
        }
    }

    private fun uploadOneClipText(serverUrl: String, text: String) {
        val requestBody = json.encodeToString(OneClipUploadTextRequest(text))
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(oneClipUrl(serverUrl, "/api/upload"))
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OneClip text upload failed: ${response.code} ${response.message}")
            }

            val resultBody = response.body?.string().orEmpty()
            val result = runCatching { json.decodeFromString<OneClipUploadResponse>(resultBody) }.getOrNull()
            if (result != null && !result.status.equals("success", ignoreCase = true)) {
                throw IOException(result.message.ifBlank { "OneClip text upload failed" })
            }
        }
    }

    private fun uploadOneClipImage(serverUrl: String, bytes: ByteArray) {
        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val requestBody = json.encodeToString(OneClipUploadImageRequest(base64Image))
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(oneClipUrl(serverUrl, "/api/upload-image"))
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OneClip image upload failed: ${response.code} ${response.message}")
            }

            val resultBody = response.body?.string().orEmpty()
            val result = runCatching { json.decodeFromString<OneClipUploadResponse>(resultBody) }.getOrNull()
            if (result != null && !result.status.equals("success", ignoreCase = true)) {
                throw IOException(result.message.ifBlank { "OneClip image upload failed" })
            }
        }
    }

    private fun primeOneClipSession(serverUrl: String) {
        val warmupPaths = listOf("/", "/api/current")
        for (path in warmupPaths) {
            val request = Request.Builder()
                .url(oneClipUrl(serverUrl, path))
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("OneClip warmup failed for $path: ${response.code} ${response.message}")
                }
            }
        }
    }

    private fun downloadOneClipImage(
        context: Context,
        serverUrl: String,
        itemId: String,
        timestamp: Double,
        downloadDirUri: Uri?
    ): ClipboardData {
        val request = Request.Builder()
            .url(oneClipUrl(serverUrl, "/api/image/$itemId"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download OneClip image: ${response.code} ${response.message}")
            }

            val mimeType = response.body?.contentType()?.toString().orEmpty().ifBlank { "image/png" }
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val fileName = buildOneClipImageFileName(itemId, timestamp, mimeType)
            val savedUri = saveIncomingBytes(
                context = context,
                dirUri = downloadDirUri,
                fileName = fileName,
                bytes = bytes,
                mimeType = mimeType
            )

            return ClipboardData(
                id = itemId,
                type = "Image",
                text = savedUri?.toString().orEmpty(),
                hash = itemId,
                hasData = true,
                dataName = fileName,
                size = bytes.size.toLong(),
                mimeType = mimeType
            )
        }
    }

    fun saveIncomingBytes(
        context: Context,
        dirUri: Uri?,
        fileName: String,
        bytes: ByteArray,
        mimeType: String = "*/*"
    ): Uri? {
        if (dirUri != null) {
            val savedInConfiguredDir = saveFile(
                context = context,
                dirUri = dirUri,
                fileName = fileName,
                bytes = bytes,
                mimeType = mimeType
            )
            if (savedInConfiguredDir != null) {
                return savedInConfiguredDir
            }
            Log.w(TAG, "[Pull] Failed to save file in configured directory, falling back to app cache")
        } else {
            Log.i(TAG, "[Pull] No download directory configured, saving incoming file to app cache")
        }
        return saveFileInAppCache(
            context = context,
            fileName = fileName,
            bytes = bytes
        )
    }

    fun buildClipCascadeImageFileName(filename: String?): String {
        val normalized = filename
            ?.substringAfterLast('/')
            ?.trim()
            .orEmpty()
        if (normalized.isNotEmpty()) {
            return normalized
        }
        return "ClipCascade-${System.currentTimeMillis()}.png"
    }

    fun guessClipCascadeImageMimeType(filename: String?): String {
        val extension = filename
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/png"
        }
    }

    fun buildClipCascadeClipboardData(
        context: Context,
        content: String
    ): ClipCascadeClipboardData {
        val uri = content.toClipboardUriOrNull()
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val fileName = getFileName(context, uri) ?: "clipcascade-file"
                val payloadType = if (isImageUri(context, uri)) {
                    "image"
                } else {
                    "file_eager"
                }
                return ClipCascadeClipboardData(
                    payload = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    type = payloadType,
                    filename = fileName
                )
            }
        }

        return ClipCascadeClipboardData(
            payload = content,
            type = "text"
        )
    }

    private fun saveFile(
        context: Context,
        dirUri: Uri,
        fileName: String,
        bytes: ByteArray,
        mimeType: String = "*/*"
    ): Uri? {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri)
            if (dir != null && dir.isDirectory) {
                val targetFile = resolveWritableTargetFile(
                    context = context,
                    dir = dir,
                    fileName = fileName,
                    bytes = bytes,
                    mimeType = mimeType
                )
                if (targetFile != null) {
                    writeBytes(context, targetFile.uri, bytes)
                    waitUntilReadable(context, targetFile.uri, bytes.size)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Pull] Failed to save file", e)
            null
        }
    }

    private fun saveFileInAppCache(
        context: Context,
        fileName: String,
        bytes: ByteArray
    ): Uri? {
        return try {
            val cacheRoot = File(context.cacheDir, CLIPBOARD_CACHE_DIR).apply { mkdirs() }
            val normalizedName = sanitizeFileName(fileName)
            val targetFile = resolveWritableCacheTargetFile(cacheRoot, normalizedName, bytes)
            targetFile.outputStream().use {
                it.write(bytes)
                it.flush()
            }
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_SUFFIX,
                targetFile
            )
            waitUntilReadable(context, uri, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "[Pull] Failed to save file in app cache", e)
            null
        }
    }

    private fun resolveWritableCacheTargetFile(cacheRoot: File, fileName: String, bytes: ByteArray): File {
        val existing = File(cacheRoot, fileName)
        if (!existing.exists()) {
            return existing
        }
        if (runCatching { existing.readBytes().contentEquals(bytes) }.getOrDefault(false)) {
            return existing
        }
        val contentHash = HashUtils.sha256(bytes).take(8)
        for (attempt in 0..99) {
            val candidate = File(cacheRoot, buildCollisionSafeFileName(fileName, contentHash, attempt))
            if (!candidate.exists()) {
                return candidate
            }
            if (runCatching { candidate.readBytes().contentEquals(bytes) }.getOrDefault(false)) {
                return candidate
            }
        }
        throw IOException("Unable to allocate cache file for $fileName")
    }

    private fun sanitizeFileName(fileName: String): String {
        val normalized = fileName
            .substringAfterLast('/')
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.')
        return normalized.ifBlank { "clipboard-${System.currentTimeMillis()}.bin" }
    }

    private fun resolveWritableTargetFile(
        context: Context,
        dir: DocumentFile,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): DocumentFile? {
        val existingFile = dir.findFile(fileName)?.takeIf { it.isFile }
        if (existingFile == null) {
            return dir.createFile(mimeType, fileName)
        }
        if (contentMatchesExisting(context, existingFile.uri, bytes)) {
            return existingFile
        }

        val contentHash = HashUtils.sha256(bytes).take(8)
        for (attempt in 0..99) {
            val candidateName = buildCollisionSafeFileName(fileName, contentHash, attempt)
            val candidateFile = dir.findFile(candidateName)?.takeIf { it.isFile }
            if (candidateFile == null) {
                Log.i(TAG, "[Pull] Avoiding in-place overwrite for $fileName, creating $candidateName instead")
                return dir.createFile(mimeType, candidateName)
            }
            if (contentMatchesExisting(context, candidateFile.uri, bytes)) {
                return candidateFile
            }
        }

        throw IOException("Unable to find writable SAF target for $fileName")
    }

    private fun buildCollisionSafeFileName(
        fileName: String,
        contentHash: String,
        attempt: Int
    ): String {
        val lastDot = fileName.lastIndexOf('.')
        val hasExtension = lastDot > 0 && lastDot < fileName.length - 1
        val baseName = if (hasExtension) fileName.substring(0, lastDot) else fileName
        val extension = if (hasExtension) fileName.substring(lastDot + 1) else ""
        val suffix = if (attempt == 0) contentHash else "$contentHash-$attempt"
        val normalizedBase = baseName.ifBlank { "clip" }
        return if (extension.isNotEmpty()) {
            "$normalizedBase-$suffix.$extension"
        } else {
            "$normalizedBase-$suffix"
        }
    }

    private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
        val resolver = context.contentResolver
        val stream = runCatching { resolver.openOutputStream(uri, "rwt") }
            .getOrNull()
            ?: resolver.openOutputStream(uri, "wt")
            ?: throw IOException("Failed to open output stream for $uri")

        stream.use {
            it.write(bytes)
            it.flush()
        }
    }

    private fun contentMatchesExisting(context: Context, uri: Uri, bytes: ByteArray): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var offset = 0
                while (offset < bytes.size) {
                    val toRead = minOf(buffer.size, bytes.size - offset)
                    val read = input.read(buffer, 0, toRead)
                    if (read != toRead) {
                        return@use false
                    }
                    for (index in 0 until read) {
                        if (buffer[index] != bytes[offset + index]) {
                            return@use false
                        }
                    }
                    offset += read
                }
                input.read() == -1
            } == true
        }.getOrDefault(false)
    }

    private fun waitUntilReadable(context: Context, uri: Uri, expectedSize: Int): Uri? {
        repeat(SAVED_URI_READY_RETRY_COUNT) { attempt ->
            val isReadable = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    var total = 0
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        total += count
                        if (total >= expectedSize) break
                    }
                    if (expectedSize <= 0) {
                        total == 0 && input.read() == -1
                    } else {
                        total >= expectedSize
                    }
                } == true
            }.getOrDefault(false)
            if (isReadable) {
                return uri
            }
            if (attempt < SAVED_URI_READY_RETRY_COUNT - 1) {
                Thread.sleep(SAVED_URI_READY_RETRY_DELAY_MS)
            }
        }
        Log.w(TAG, "[Pull] Saved file is not readable yet: $uri")
        return null
    }

    private fun buildOneClipImageFileName(itemId: String, timestamp: Double, mimeType: String): String {
        val normalizedSubtype = mimeType
            .substringAfter('/', "png")
            .substringBefore(';')
            .substringBefore('+')
            .lowercase(Locale.ROOT)
            .let {
                when (it) {
                    "jpeg" -> "jpg"
                    "svg+xml" -> "svg"
                    else -> it.ifBlank { "png" }
                }
            }
        val unixSeconds = timestamp.toLong().takeIf { it > 0 } ?: System.currentTimeMillis() / 1000
        return "OneClip-$unixSeconds-$itemId.$normalizedSubtype"
    }

    private fun querySyncClipboardHistoryPage(
        serverUrl: String,
        username: String,
        pass: String,
        page: Int,
        modifiedAfter: String?
    ): List<SyncClipboardHistoryRecord>? {
        val (baseUrl, _) = getBaseAndJsonUrl(serverUrl)
        val credential = Credentials.basic(username, pass)
        val formBody = FormBody.Builder()
            .add("page", page.toString())
            .apply {
                modifiedAfter
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add("modifiedAfter", it) }
            }
            .build()

        val request = Request.Builder()
            .url("${baseUrl}api/history/query")
            .header("Authorization", credential)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404 || response.code == 405) {
                Log.d(TAG, "[Pull] SyncClipboard history API is unavailable on this server")
                return null
            }
            if (!response.isSuccessful) {
                throw IOException("Failed to query SyncClipboard history: ${response.code} ${response.message}")
            }

            val body = response.body?.string().orEmpty()
            return json.decodeFromString(body)
        }
    }

    private fun buildBootstrapHistoryCursor(
        records: List<SyncClipboardHistoryRecord>
    ): SyncClipboardHistoryCursor? {
        val newestModifiedAt = records.maxOfOrNull(::historyRecordModifiedAtMillis) ?: return null
        val seenIds = records
            .filter { historyRecordModifiedAtMillis(it) == newestModifiedAt }
            .map(::syncClipboardHistoryProfileId)
            .distinct()
        return buildHistoryCursor(newestModifiedAt, seenIds)
    }

    private fun filterAndSortHistoryRecords(
        records: List<SyncClipboardHistoryRecord>,
        cursor: SyncClipboardHistoryCursor
    ): List<SyncClipboardHistoryRecord> {
        val cursorMillis = parseIsoTimestampMillis(cursor.modifiedAfter)
        val seenIds = cursor.seenProfileIdsAtModifiedAfter.toHashSet()
        return records
            .distinctBy(::syncClipboardHistoryProfileId)
            .asSequence()
            .filter { record ->
                val modifiedAt = historyRecordModifiedAtMillis(record)
                when {
                    modifiedAt > cursorMillis -> true
                    modifiedAt < cursorMillis -> false
                    else -> syncClipboardHistoryProfileId(record) !in seenIds
                }
            }
            .sortedWith(
                compareBy<SyncClipboardHistoryRecord>(
                    ::historyRecordModifiedAtMillis,
                    ::historyRecordCreateAtMillis
                ).thenBy(::syncClipboardHistoryProfileId)
            )
            .toList()
    }

    private fun advanceSyncClipboardHistoryCursor(
        current: SyncClipboardHistoryCursor,
        records: List<SyncClipboardHistoryRecord>
    ): SyncClipboardHistoryCursor {
        var latestModifiedAt = parseIsoTimestampMillis(current.modifiedAfter)
        val seenIds = current.seenProfileIdsAtModifiedAfter.toMutableSet()
        for (record in records) {
            val modifiedAt = historyRecordModifiedAtMillis(record)
            val profileId = syncClipboardHistoryProfileId(record)
            when {
                modifiedAt > latestModifiedAt -> {
                    latestModifiedAt = modifiedAt
                    seenIds.clear()
                    seenIds += profileId
                }

                modifiedAt == latestModifiedAt -> {
                    seenIds += profileId
                }
            }
        }
        return buildHistoryCursor(latestModifiedAt, seenIds.toList())
    }

    private fun buildHistoryCursor(
        modifiedAfterMillis: Long,
        seenIds: List<String>
    ): SyncClipboardHistoryCursor {
        return SyncClipboardHistoryCursor(
            modifiedAfter = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(modifiedAfterMillis),
                ZoneOffset.UTC
            ).toString(),
            seenProfileIdsAtModifiedAfter = seenIds.distinct().sorted()
        )
    }

    private fun encodeSyncClipboardHistoryCursor(cursor: SyncClipboardHistoryCursor): String {
        val serialized = json.encodeToString(cursor)
        return REVISION_SYNCCLIPBOARD_HISTORY_PREFIX + Base64.encodeToString(
            serialized.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    private fun decodeSyncClipboardHistoryCursor(revision: String?): SyncClipboardHistoryCursor? {
        val encoded = revision
            ?.takeIf { it.startsWith(REVISION_SYNCCLIPBOARD_HISTORY_PREFIX) }
            ?.removePrefix(REVISION_SYNCCLIPBOARD_HISTORY_PREFIX)
            ?: return null
        return runCatching {
            val jsonText = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            json.decodeFromString<SyncClipboardHistoryCursor>(jsonText)
        }.getOrNull()
    }

    private fun historyRecordToClipboardData(record: SyncClipboardHistoryRecord): ClipboardData {
        return ClipboardData(
            id = syncClipboardHistoryProfileId(record),
            type = record.type,
            text = record.text,
            hash = record.hash,
            hasData = record.hasData,
            size = record.size,
            remoteTimestamp = historyRecordModifiedAtMillis(record)
        )
    }

    private fun downloadSyncClipboardHistoryData(
        context: Context,
        serverUrl: String,
        username: String,
        pass: String,
        record: SyncClipboardHistoryRecord,
        metadata: ClipboardData,
        downloadDirUri: Uri?
    ): ClipboardData {
        val (baseUrl, _) = getBaseAndJsonUrl(serverUrl)
        val credential = Credentials.basic(username, pass)
        val profileId = syncClipboardHistoryProfileId(record)
        val request = Request.Builder()
            .url("${baseUrl}api/history/$profileId/data")
            .header("Authorization", credential)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download SyncClipboard history data: ${response.code} ${response.message}")
            }

            val mimeType = response.body?.contentType()?.toString().orEmpty().ifBlank { "application/octet-stream" }
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val fileName = extractSyncClipboardHistoryFileName(
                response.header("Content-Disposition"),
                record,
                mimeType
            )

            if (record.type.equals("Text", ignoreCase = true)) {
                return metadata.copy(
                    text = String(bytes, Charsets.UTF_8),
                    dataName = fileName,
                    size = bytes.size.toLong(),
                    mimeType = "text/plain"
                )
            }

            val savedUri = saveIncomingBytes(
                context = context,
                dirUri = downloadDirUri,
                fileName = fileName,
                bytes = bytes,
                mimeType = mimeType
            )
            return metadata.copy(
                text = savedUri?.toString().orEmpty(),
                dataName = fileName,
                size = bytes.size.toLong(),
                mimeType = mimeType
            )
        }
    }

    private fun extractSyncClipboardHistoryFileName(
        contentDisposition: String?,
        record: SyncClipboardHistoryRecord,
        mimeType: String
    ): String {
        contentDisposition
            ?.substringAfter("filename*=", "")
            ?.takeIf { it.isNotBlank() }
            ?.substringBefore(';')
            ?.trim()
            ?.trim('"')
            ?.substringAfter("''", "")
            ?.let {
                return URLDecoder.decode(it, Charsets.UTF_8.name())
            }

        contentDisposition
            ?.substringAfter("filename=", "")
            ?.takeIf { it.isNotBlank() }
            ?.substringBefore(';')
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val baseName = "SyncClipboard-${record.hash.lowercase(Locale.ROOT)}"
        if (record.type.equals("Image", ignoreCase = true) || mimeType.startsWith("image/")) {
            return "$baseName.${extensionFromMimeType(mimeType, "png")}"
        }
        return "$baseName.${extensionFromMimeType(mimeType, "bin")}"
    }

    private fun extensionFromMimeType(mimeType: String, fallback: String): String {
        return mimeType
            .substringAfter('/', fallback)
            .substringBefore(';')
            .substringBefore('+')
            .lowercase(Locale.ROOT)
            .let {
                when (it) {
                    "jpeg" -> "jpg"
                    "plain" -> "txt"
                    else -> it.ifBlank { fallback }
                }
            }
    }

    private fun syncClipboardHistoryProfileId(record: SyncClipboardHistoryRecord): String {
        return "${record.type}-${record.hash}"
    }

    private fun historyRecordModifiedAtMillis(record: SyncClipboardHistoryRecord): Long {
        return parseIsoTimestampMillis(record.lastModified.ifBlank { record.createTime })
    }

    private fun historyRecordCreateAtMillis(record: SyncClipboardHistoryRecord): Long {
        return parseIsoTimestampMillis(record.createTime.ifBlank { record.lastModified })
    }

    private fun parseIsoTimestampMillis(value: String): Long {
        if (value.isBlank()) {
            return 0L
        }
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrElse { 0L }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)
            }.getOrNull()?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun isImageUri(context: Context, uri: Uri): Boolean {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            ?: getFileName(context, uri)?.substringAfterLast('.', "")?.let { extension ->
                when (extension.lowercase(Locale.ROOT)) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> null
                }
            }

        return mimeType?.startsWith("image/") == true
    }

    private fun isReadableClipboardUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } == true
        }.getOrDefault(false)
    }

    private fun readClipboardUriBytes(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    private fun String.toClipboardUriOrNull(): Uri? {
        return if (startsWith("content://") || startsWith("file://")) Uri.parse(this) else null
    }

    private fun buildRevisionToken(data: ClipboardData): String? {
        return when {
            data.id.isNotBlank() -> REVISION_ONECLIP_PREFIX + data.id
            data.hash.isNotBlank() -> REVISION_HASH_PREFIX + data.hash
            data.text.isNotBlank() -> REVISION_TEXT_PREFIX + HashUtils.sha256(data.text)
            else -> null
        }
    }

    private fun buildOneClipRevision(data: ClipboardData): String {
        val timestampPart = data.timestamp
            .takeIf { it > 0.0 }
            ?.toString()
            .orEmpty()
        val contentPart = when {
            data.text.isNotBlank() -> HashUtils.sha256(data.text)
            data.hasImage -> "image"
            else -> data.type.lowercase(Locale.ROOT)
        }
        return buildString {
            append(REVISION_ONECLIP_PREFIX)
            append(data.id)
            if (timestampPart.isNotEmpty()) {
                append(':')
                append(timestampPart)
            }
            if (contentPart.isNotEmpty()) {
                append(':')
                append(contentPart)
            }
        }
    }

    private fun resolveUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private fun getBaseAndJsonUrl(serverUrl: String): Pair<String, String> {
        var url = resolveUrl(serverUrl)
        if (url.endsWith("SyncClipboard.json", ignoreCase = true)) {
            val base = url.substringBeforeLast("SyncClipboard.json")
            return base to url
        }
        if (!url.endsWith("/")) url += "/"
        return url to "${url}SyncClipboard.json"
    }

    private fun oneClipUrl(serverUrl: String, path: String): String {
        val base = resolveUrl(serverUrl).trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return base + normalizedPath
    }

    private const val REVISION_ETAG_PREFIX = "etag:"
    private const val REVISION_HASH_PREFIX = "hash:"
    private const val REVISION_SYNCCLIPBOARD_HISTORY_PREFIX = "synchistory:"
    private const val REVISION_TEXT_PREFIX = "text:"
    private const val REVISION_ONECLIP_PREFIX = "oneclip:"
}
