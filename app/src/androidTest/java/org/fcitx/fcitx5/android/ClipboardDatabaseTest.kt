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
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardContentAnalyzer
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
    fun migration5To6PreservesEntriesAndMarksThemForClassification() {
        val name = "clipboard-category-migration-test"
        migrationHelper.createDatabase(name, 5).apply {
            execSQL(
                "INSERT INTO clipboard (id, text, pinned, timestamp, type, deleted, sensitive, favorite) " +
                    "VALUES (1, 'person@example.com', 1, 123, 'text/plain', 0, 1, 1)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(name, 6, true).apply {
            query("SELECT * FROM clipboard WHERE id=1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("person@example.com", cursor.getString(cursor.getColumnIndexOrThrow("text")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("pinned")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("favorite")))
                assertEquals("Other", cursor.getString(cursor.getColumnIndexOrThrow("category")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("classificationVersion")))
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
    fun categoryFiltersComposeWithFavoritesAndOrdering() = runBlocking {
        insert("old phone", 10, category = ClipboardCategory.Phone)
        insert("new phone", 30, category = ClipboardCategory.Phone)
        insert("pinned phone", 20, pinned = true, category = ClipboardCategory.Phone)
        insert("favorite phone", 40, favorite = true, category = ClipboardCategory.Phone)
        insert("favorite email", 50, favorite = true, category = ClipboardCategory.Email)

        val phones = dao.allEntries(ClipboardCategory.Phone).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page<Int, ClipboardEntry>
        assertEquals(
            listOf("pinned phone", "favorite phone", "new phone", "old phone"),
            phones.data.map { it.text }
        )

        val favoritePhones = dao.favoriteEntries(ClipboardCategory.Phone).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page<Int, ClipboardEntry>
        assertEquals(listOf("favorite phone"), favoritePhones.data.map { it.text })
    }

    @Test
    fun outdatedEntriesCanBeReclassifiedWithoutChangingProtection() = runBlocking {
        val id = insert(
            "person@example.com",
            10,
            pinned = true,
            favorite = true,
            category = ClipboardCategory.Other,
            classificationVersion = 0
        )
        assertEquals(listOf(id), dao.entriesToClassify(ClipboardContentAnalyzer.VERSION, 100).map { it.id })

        dao.updateClassification(id, ClipboardCategory.Email, ClipboardContentAnalyzer.VERSION)

        val updated = dao.get(id)!!
        assertEquals(ClipboardCategory.Email, updated.category)
        assertEquals(ClipboardContentAnalyzer.VERSION, updated.classificationVersion)
        assertTrue(updated.pinned)
        assertTrue(updated.favorite)
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
    fun duplicateUpdateReclassifiesAndPreservesProtection() = runBlocking {
        val id = insert(
            text = "duplicate",
            timestamp = 1,
            pinned = true,
            favorite = true
        )

        dao.updateTimeAndClassification(
            id,
            99,
            ClipboardCategory.Otp,
            ClipboardContentAnalyzer.VERSION
        )

        val updated = dao.get(id)!!
        assertEquals(99, updated.timestamp)
        assertEquals(ClipboardCategory.Otp, updated.category)
        assertEquals(ClipboardContentAnalyzer.VERSION, updated.classificationVersion)
        assertTrue(updated.pinned)
        assertTrue(updated.favorite)
    }

    private suspend fun insert(
        text: String,
        timestamp: Long,
        pinned: Boolean = false,
        favorite: Boolean = false,
        category: ClipboardCategory = ClipboardCategory.Other,
        classificationVersion: Int = ClipboardContentAnalyzer.VERSION
    ): Int = dao.insert(
        ClipboardEntry(
            text = text,
            pinned = pinned,
            timestamp = timestamp,
            favorite = favorite,
            category = category,
            classificationVersion = classificationVersion
        )
    ).toInt()
}
