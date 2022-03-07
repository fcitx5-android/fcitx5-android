package me.rocka.fcitx5test.data.clipboard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClipboardDao {
    @Insert
    suspend fun insertAll(vararg clipboardEntry: ClipboardEntry)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET pinned=:pinned WHERE id=:id")
    suspend fun updatePinStatus(id: Int, pinned: Boolean)

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun getAll(): List<ClipboardEntry>

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE id<:id")
    suspend fun deleteIdLessThan(id: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME}")
    suspend fun deleteAll()
}