package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClipboardDao {
    @Insert
    suspend fun insert(clipboardEntry: ClipboardEntry): Long

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET pinned=:pinned WHERE id=:id")
    suspend fun updatePinStatus(id: Int, pinned: Boolean)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET text=:text WHERE id=:id")
    suspend fun updateText(id: Int, text: String)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET timestamp=:timestamp WHERE id=:id")
    suspend fun updateTime(id: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun itemCount(): Int

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id LIMIT 1")
    suspend fun get(id: Int): ClipboardEntry?

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE rowId=:rowId LIMIT 1")
    suspend fun get(rowId: Long): ClipboardEntry?

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun getAll(): List<ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE text=:text LIMIT 1")
    suspend fun find(text: String): ClipboardEntry?

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE timestamp<:timestamp AND NOT pinned")
    suspend fun deleteUnpinnedIdLessThan(timestamp: Long)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE NOT pinned")
    suspend fun deleteAllUnpinned()

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun deleteAll()
}