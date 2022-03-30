package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClipboardDao {
    @Insert
    suspend fun insertAll(vararg clipboardEntry: ClipboardEntry)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET pinned=:pinned WHERE id=:id")
    suspend fun updatePinStatus(id: Int, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun itemCount(): Int

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun getAll(): List<ClipboardEntry>

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE id<:id AND NOT pinned")
    suspend fun deleteUnpinnedIdLessThan(id: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE NOT pinned")
    suspend fun deleteAllUnpinned()

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun deleteAll()
}