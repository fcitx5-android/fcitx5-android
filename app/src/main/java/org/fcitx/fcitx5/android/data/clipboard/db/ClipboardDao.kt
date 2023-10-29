/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.paging.PagingSource
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

    @Query("SELECT COUNT(*) FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0")
    suspend fun itemCount(): Int

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id AND deleted=0 LIMIT 1")
    suspend fun get(id: Int): ClipboardEntry?

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE rowId=:rowId AND deleted=0 LIMIT 1")
    suspend fun get(rowId: Long): ClipboardEntry?

    @Query("SELECT EXISTS(SELECT 1 FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND deleted=0)")
    suspend fun haveUnpinned(): Boolean

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND deleted=0")
    suspend fun getAllUnpinned(): List<ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0 ORDER BY pinned DESC, timestamp DESC")
    fun allEntries(): PagingSource<Int, ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE text=:text AND deleted=0 LIMIT 1")
    suspend fun find(text: String): ClipboardEntry?

    @Query("SELECT id FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0")
    suspend fun findAllIds(): IntArray

    @Query("SELECT id FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND deleted=0")
    suspend fun findUnpinnedIds(): IntArray

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET deleted=1 WHERE id in (:ids)")
    suspend fun markAsDeleted(vararg ids: Int)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET DELETED=1 WHERE timestamp<:timestamp AND pinned=0 AND deleted=0")
    suspend fun markUnpinnedAsDeletedEarlierThan(timestamp: Long)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET deleted=0 WHERE id in (:ids) AND deleted=1")
    suspend fun undoDelete(vararg ids: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=1")
    suspend fun realDelete()
}