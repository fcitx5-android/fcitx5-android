/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory

@Dao
interface ClipboardDao {
    @Insert
    suspend fun insert(clipboardEntry: ClipboardEntry): Long

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET pinned=:pinned WHERE id=:id")
    suspend fun updatePinStatus(id: Int, pinned: Boolean)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET favorite=:favorite WHERE id=:id")
    suspend fun updateFavoriteStatus(id: Int, favorite: Boolean)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET text=:text, category=:category, classificationVersion=:classificationVersion WHERE id=:id")
    suspend fun updateTextAndClassification(
        id: Int,
        text: String,
        category: ClipboardCategory,
        classificationVersion: Int
    )

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET timestamp=:timestamp, category=:category, classificationVersion=:classificationVersion WHERE id=:id")
    suspend fun updateTimeAndClassification(
        id: Int,
        timestamp: Long,
        category: ClipboardCategory,
        classificationVersion: Int
    )

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET category=:category, classificationVersion=:classificationVersion WHERE id=:id")
    suspend fun updateClassification(
        id: Int,
        category: ClipboardCategory,
        classificationVersion: Int
    )

    @Query("SELECT COUNT(*) FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0")
    suspend fun itemCount(): Int

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE id=:id AND deleted=0 LIMIT 1")
    suspend fun get(id: Int): ClipboardEntry?

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE rowId=:rowId AND deleted=0 LIMIT 1")
    suspend fun get(rowId: Long): ClipboardEntry?

    @Query("SELECT EXISTS(SELECT 1 FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND favorite=0 AND deleted=0)")
    suspend fun haveDeletable(): Boolean

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND favorite=0 AND deleted=0")
    suspend fun getAllUnprotected(): List<ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0 AND (:category IS NULL OR category=:category) ORDER BY pinned DESC, timestamp DESC")
    fun allEntries(category: ClipboardCategory? = null): PagingSource<Int, ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE favorite=1 AND deleted=0 AND (:category IS NULL OR category=:category) ORDER BY pinned DESC, timestamp DESC")
    fun favoriteEntries(category: ClipboardCategory? = null): PagingSource<Int, ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=0 AND classificationVersion<:classificationVersion LIMIT :limit")
    suspend fun entriesToClassify(
        classificationVersion: Int,
        limit: Int
    ): List<ClipboardEntry>

    @Query("SELECT * FROM ${ClipboardEntry.TABLE_NAME} WHERE text=:text AND sensitive=:sensitive AND deleted=0 LIMIT 1")
    suspend fun find(text: String, sensitive: Boolean = false): ClipboardEntry?

    @Query("SELECT id FROM ${ClipboardEntry.TABLE_NAME} WHERE pinned=0 AND favorite=0 AND deleted=0")
    suspend fun findDeletableIds(): IntArray

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET deleted=1 WHERE id in (:ids)")
    suspend fun markAsDeleted(vararg ids: Int)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET deleted=1 WHERE timestamp<:timestamp AND pinned=0 AND favorite=0 AND deleted=0")
    suspend fun markUnprotectedAsDeletedEarlierThan(timestamp: Long)

    @Query("UPDATE ${ClipboardEntry.TABLE_NAME} SET deleted=0 WHERE id in (:ids) AND deleted=1")
    suspend fun undoDelete(vararg ids: Int)

    @Query("DELETE FROM ${ClipboardEntry.TABLE_NAME} WHERE deleted=1")
    suspend fun realDelete()
}
