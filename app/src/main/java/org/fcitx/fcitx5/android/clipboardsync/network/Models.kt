package org.fcitx.fcitx5.android.clipboardsync.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ClipboardData(
    @JsonNames("id")
    val id: String = "",

    @SerialName("Type")
    @JsonNames("type")
    val type: String = "Text",

    @SerialName("Text")
    @JsonNames("text", "Clipboard", "value")
    val text: String = "",

    @SerialName("Hash")
    @JsonNames("hash")
    val hash: String = "",

    @SerialName("HasData")
    @JsonNames("hasData")
    val hasData: Boolean = false,

    @SerialName("DataName")
    @JsonNames("dataName", "File")
    val dataName: String = "",

    @SerialName("Size")
    @JsonNames("size")
    val size: Long = 0,

    @JsonNames("hasImage")
    val hasImage: Boolean = false,

    @JsonNames("timestamp")
    val timestamp: Double = 0.0,

    val remoteTimestamp: Long = 0L,

    val mimeType: String = ""
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SyncClipboardHistoryRecord(
    @JsonNames("hash", "Hash")
    val hash: String = "",

    @JsonNames("text", "Text")
    val text: String = "",

    @JsonNames("type", "Type")
    val type: String = "Text",

    @JsonNames("createTime", "CreateTime")
    val createTime: String = "",

    @JsonNames("lastModified", "LastModified")
    val lastModified: String = "",

    @JsonNames("lastAccessed", "LastAccessed")
    val lastAccessed: String = "",

    @JsonNames("starred", "Starred")
    val starred: Boolean = false,

    @JsonNames("pinned", "Pinned")
    val pinned: Boolean = false,

    @JsonNames("size", "Size")
    val size: Long = 0,

    @JsonNames("hasData", "HasData")
    val hasData: Boolean = false,

    @JsonNames("version", "Version")
    val version: Int = 0,

    @JsonNames("isDeleted", "IsDeleted")
    val isDeleted: Boolean = false
)

@Serializable
data class SyncClipboardHistoryCursor(
    val modifiedAfter: String = "",
    val seenProfileIdsAtModifiedAfter: List<String> = emptyList()
)

@Serializable
data class OneClipHistoryResponse(
    val items: List<OneClipHistoryItem> = emptyList()
)

@Serializable
data class OneClipHistoryItem(
    val id: String = "",
    val type: String = "text",
    val preview: String = "",
    val hasImage: Boolean = false,
    val timestamp: Double = 0.0
)

@Serializable
data class OneClipUploadTextRequest(
    val text: String
)

@Serializable
data class OneClipUploadImageRequest(
    val image: String
)

@Serializable
data class OneClipUploadResponse(
    val status: String = "",
    val message: String = ""
)

@Serializable
data class OneClipEventData(
    val update: Boolean = false,
    val id: String = ""
)

@Serializable
data class ClipCascadeClipboardData(
    val payload: String = "",
    val type: String = "text",
    val filename: String? = null
)

@Serializable
data class ClipCascadeSessionValidationResponse(
    val valid: Boolean = false
)

@Serializable
data class ClipCascadeUserInfoResponse(
    val salt: String = "",
    @SerialName("hash_rounds")
    val hashRounds: Int = 100000
)

@Serializable
data class ClipCascadeEncryptedPayload(
    val nonce: String = "",
    val ciphertext: String = "",
    val tag: String = ""
)
