/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard.db

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fcitx.fcitx5.android.common.ClipboardMetadata
import org.fcitx.fcitx5.android.utils.timestamp

@Entity(tableName = ClipboardEntry.TABLE_NAME)
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    @ColumnInfo(defaultValue = "")
    val originalText: String = "",
    @ColumnInfo(defaultValue = "")
    val originalRootUri: String = "",
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "-1")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = ClipDescription.MIMETYPE_TEXT_PLAIN)
    val type: String = ClipDescription.MIMETYPE_TEXT_PLAIN,
    @ColumnInfo(defaultValue = ClipboardMetadata.SOURCE_LOCAL)
    val source: String = ClipboardMetadata.SOURCE_LOCAL,
    @ColumnInfo(defaultValue = "0")
    val deleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val sensitive: Boolean = false
) {
    companion object {
        const val BULLET = "•"

        const val TABLE_NAME = "clipboard"
        const val SOURCE_LOCAL = ClipboardMetadata.SOURCE_LOCAL
        const val SOURCE_REMOTE = ClipboardMetadata.SOURCE_REMOTE

        private val IS_SENSITIVE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }

        fun fromClipData(
            clipData: ClipData,
            transformer: ((String) -> String)? = null
        ): ClipboardEntry? {
            val desc = clipData.description
            // TODO: handle multiple items (when does this happen?)
            val item = clipData.getItemAt(0) ?: return null
            val str = item.text?.toString()
                ?: item.uri?.toString()
                ?: return null
            val sensitive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                desc.extras?.getBoolean(IS_SENSITIVE) ?: false
            } else {
                false
            }
            val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                desc.extras?.getString(ClipboardMetadata.EXTRA_SOURCE)
                    ?.takeIf { it == SOURCE_REMOTE }
                    ?: SOURCE_LOCAL
            } else {
                SOURCE_LOCAL
            }
            val originalRootUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && source == SOURCE_REMOTE) {
                desc.extras?.getString(ClipboardMetadata.EXTRA_REMOTE_ROOT_URI).orEmpty()
            } else {
                ""
            }
            return ClipboardEntry(
                text = if (transformer != null) transformer(str) else str,
                originalText = "",
                originalRootUri = originalRootUri,
                timestamp = clipData.timestamp(),
                type = desc.getMimeType(0),
                source = source,
                sensitive = sensitive
            )
        }
    }

    fun isUriEntry(): Boolean {
        return text.startsWith("content://") || text.startsWith("file://")
    }

    fun isRemoteTextEntry(): Boolean {
        return source == SOURCE_REMOTE && !isUriEntry()
    }
}
