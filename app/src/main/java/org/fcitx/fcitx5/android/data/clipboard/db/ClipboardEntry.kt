/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard.db

import android.content.ClipData
import android.content.ClipDescription
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = ClipboardEntry.TABLE_NAME)
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "-1")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = ClipDescription.MIMETYPE_TEXT_PLAIN)
    val type: String = ClipDescription.MIMETYPE_TEXT_PLAIN,
    @ColumnInfo(defaultValue = "0")
    val deleted: Boolean = false,
) {
    companion object {
        const val TABLE_NAME = "clipboard"

        fun fromClipData(
            clipData: ClipData,
            transformer: ((String) -> String)? = null
        ): ClipboardEntry? {
            val str = clipData.getItemAt(0).text?.toString() ?: return null
            return ClipboardEntry(
                text = transformer?.let { it(str) } ?: str,
                type = clipData.description.getMimeType(0)
            )
        }
    }
}