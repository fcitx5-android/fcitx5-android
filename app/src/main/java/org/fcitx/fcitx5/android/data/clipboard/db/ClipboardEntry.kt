package org.fcitx.fcitx5.android.data.clipboard.db

import android.content.ClipData
import android.content.ClipDescription
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = ClipboardEntry.TABLE_NAME)
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val pinned: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "clipboard"

        fun fromClipData(clipData: ClipData): ClipboardEntry? = clipData.takeIf {
            it.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
        }?.run { ClipboardEntry(text = getItemAt(0).text.toString()) }
    }
}