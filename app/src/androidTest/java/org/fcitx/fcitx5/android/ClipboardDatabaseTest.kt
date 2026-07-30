/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ClipboardDatabaseTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClipboardDatabase::class.java
    )

    private lateinit var database: ClipboardDatabase
    private lateinit var dao: ClipboardDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ClipboardDatabase::class.java
        ).build()
        dao = database.clipboardDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun migration4To5PreservesEntriesAndDefaultsFavoriteToFalse() {
        val name = "clipboard-migration-test"
        migrationHelper.createDatabase(name, 4).apply {
            execSQL(
                "INSERT INTO clipboard (id, text, pinned, timestamp, type, deleted, sensitive) " +
                    "VALUES (1, 'kept', 1, 123, 'text/plain', 0, 1)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(name, 5, true).apply {
            query("SELECT * FROM clipboard WHERE id=1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("kept", cursor.getString(cursor.getColumnIndexOrThrow("text")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("pinned")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("favorite")))
            }
            close()
        }
    }

    @Test
    fun favoritesAreFilteredAndKeepPinnedThenTimestampOrdering() = runBlocking {
        insert(text = "old favorite", timestamp = 10, favorite = true)
        insert(text = "new favorite", timestamp = 30, favorite = true)
        insert(text = "pinned favorite", timestamp = 20, pinned = true, favorite = true)
        insert(text = "not favorite", timestamp = 40)

        val result = dao.favoriteEntries().load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )
        assertTrue(result is PagingSource.LoadResult.Page)
        val entries = result as PagingSource.LoadResult.Page<Int, ClipboardEntry>

        assertEquals(
            listOf("pinned favorite", "new favorite", "old favorite"),
            entries.data.map { it.text }
        )
    }

    @Test
    fun automaticDeletionTargetsOnlyUnprotectedEntries() = runBlocking {
        val deletableId = insert(text = "deletable", timestamp = 10)
        insert(text = "pinned", timestamp = 20, pinned = true)
        insert(text = "favorite", timestamp = 30, favorite = true)
        insert(text = "both", timestamp = 40, pinned = true, favorite = true)

        assertTrue(dao.haveDeletable())
        assertArrayEquals(intArrayOf(deletableId), dao.findDeletableIds())

        dao.markUnprotectedAsDeletedEarlierThan(Long.MAX_VALUE)

        assertNull(dao.get(deletableId))
        assertEquals(3, dao.itemCount())
        assertFalse(dao.haveDeletable())
    }

    @Test
    fun favoriteCanStillBeDeletedAndRestoredExplicitly() = runBlocking {
        val id = insert(text = "favorite", timestamp = 10, favorite = true)

        dao.markAsDeleted(id)
        assertNull(dao.get(id))

        dao.undoDelete(id)
        assertTrue(dao.get(id)?.favorite == true)
    }

    @Test
    fun favoriteStatusCanBeToggled() = runBlocking {
        val id = insert(text = "entry", timestamp = 10)

        dao.updateFavoriteStatus(id, true)
        assertTrue(dao.get(id)?.favorite == true)

        dao.updateFavoriteStatus(id, false)
        assertFalse(dao.get(id)?.favorite == true)
    }

    @Test
    fun updatingTimestampPreservesPinnedAndFavoriteState() = runBlocking {
        val id = insert(
            text = "duplicate",
            timestamp = 1,
            pinned = true,
            favorite = true
        )

        dao.updateTime(id, 99)

        val updated = dao.get(id)!!
        assertEquals(99, updated.timestamp)
        assertTrue(updated.pinned)
        assertTrue(updated.favorite)
    }

    private suspend fun insert(
        text: String,
        timestamp: Long,
        pinned: Boolean = false,
        favorite: Boolean = false
    ): Int = dao.insert(
        ClipboardEntry(
            text = text,
            pinned = pinned,
            timestamp = timestamp,
            favorite = favorite
        )
    ).toInt()
}
