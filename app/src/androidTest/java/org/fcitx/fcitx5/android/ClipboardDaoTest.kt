/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ClipboardDaoTest {
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

    private suspend fun insert(text: String, timestamp: Long, pinned: Boolean = false): Int =
        dao.insert(ClipboardEntry(text = text, timestamp = timestamp, pinned = pinned)).toInt()

    @Test
    fun recopyingMiddleEntryDoesNotDeleteTooMany(): Unit = runBlocking {
        // Copy A, B, C, B, D with a limit of 3.
        val a = insert("A", 1)
        val b = insert("B", 2)
        val c = insert("C", 3)
        dao.updateTime(b, 4)
        val d = insert("D", 5)

        dao.pruneUnpinned(3)

        assertEquals(3, dao.itemCount())
        assertNull(dao.get(a))
        for (id in listOf(b, c, d)) assertNotNull(dao.get(id))
    }

    @Test
    fun recopyingOldestEntryDoesNotExceedLimit(): Unit = runBlocking {
        // Copy A, B, A, C with a limit of 2.
        val a = insert("A", 1)
        val b = insert("B", 2)
        dao.updateTime(a, 3)
        val c = insert("C", 4)

        dao.pruneUnpinned(2)

        assertEquals(2, dao.itemCount())
        assertNull(dao.get(b))
        for (id in listOf(a, c)) assertNotNull(dao.get(id))
    }

    @Test
    fun equalTimestampsKeepNewestIds(): Unit = runBlocking {
        val a = insert("A", 1)
        val b = insert("B", 1)
        val c = insert("C", 1)

        dao.pruneUnpinned(2)

        assertEquals(2, dao.itemCount())
        assertNull(dao.get(a))
        for (id in listOf(b, c)) assertNotNull(dao.get(id))
    }

    @Test
    fun pinnedAndDeletedEntriesDoNotConsumeLimit(): Unit = runBlocking {
        val pinned = insert("pinned", 1, pinned = true)
        val active = insert("active", 2)
        val deleted = insert("deleted", 3)
        dao.markAsDeleted(deleted)

        dao.pruneUnpinned(1)

        assertEquals(2, dao.itemCount())
        assertNotNull(dao.get(pinned))
        assertNotNull(dao.get(active))
        assertNull(dao.get(deleted))
    }

    @Test
    fun zeroLimitDeletesAllUnpinnedIncludingFutureTimestamps(): Unit = runBlocking {
        val pinned = insert("pinned", 1, pinned = true)
        val active = insert("future", Long.MAX_VALUE)

        dao.pruneUnpinned(0)

        assertEquals(1, dao.itemCount())
        assertNotNull(dao.get(pinned))
        assertNull(dao.get(active))
    }

    @Test
    fun belowLimitKeepsAllEntries(): Unit = runBlocking {
        insert("A", 1)
        insert("B", 2)

        dao.pruneUnpinned(3)

        assertEquals(2, dao.itemCount())
    }
}
