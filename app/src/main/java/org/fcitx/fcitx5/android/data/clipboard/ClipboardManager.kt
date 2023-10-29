/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.Keep
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.clipboardManager

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao

    fun interface OnClipboardUpdateListener {
        fun onUpdate(entry: ClipboardEntry)
    }

    private val clipboardManager = appContext.clipboardManager

    private val mutex = Mutex()

    var itemCount: Int = 0
        private set

    private suspend fun updateItemCount() {
        itemCount = clbDao.itemCount()
    }

    private val onUpdateListeners = WeakHashSet<OnClipboardUpdateListener>()

    var transformer: ((String) -> String)? = null

    fun addOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.add(listener)
    }

    fun removeOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.remove(listener)
    }

    private val enabledPref = AppPrefs.getInstance().clipboard.clipboardListening

    @Keep
    private val enabledListener = ManagedPreference.OnChangeListener<Boolean> { _, value ->
        if (value) {
            clipboardManager.addPrimaryClipChangedListener(this)
        } else {
            clipboardManager.removePrimaryClipChangedListener(this)
        }
    }

    private val limitPref = AppPrefs.getInstance().clipboard.clipboardHistoryLimit

    @Keep
    private val limitListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        launch { removeOutdated() }
    }

    var lastEntry: ClipboardEntry? = null

    private fun updateLastEntry(entry: ClipboardEntry) {
        lastEntry = entry
        onUpdateListeners.forEach { it.onUpdate(entry) }
    }

    fun init(context: Context) {
        clbDb = Room
            .databaseBuilder(context, ClipboardDatabase::class.java, "clbdb")
            .build()
        clbDao = clbDb.clipboardDao()
        enabledListener.onChange(enabledPref.key, enabledPref.getValue())
        enabledPref.registerOnChangeListener(enabledListener)
        limitListener.onChange(limitPref.key, limitPref.getValue())
        limitPref.registerOnChangeListener(limitListener)
        launch { updateItemCount() }
    }

    suspend fun get(id: Int) = clbDao.get(id)

    suspend fun haveUnpinned() = clbDao.haveUnpinned()

    fun allEntries() = clbDao.allEntries()

    suspend fun pin(id: Int) = clbDao.updatePinStatus(id, true)

    suspend fun unpin(id: Int) = clbDao.updatePinStatus(id, false)

    suspend fun updateText(id: Int, text: String) {
        lastEntry?.let {
            if (id == it.id) updateLastEntry(it.copy(text = text))
        }
        clbDao.updateText(id, text)
    }

    suspend fun delete(id: Int) {
        clbDao.markAsDeleted(id)
        updateItemCount()
    }

    suspend fun deleteAll(skipPinned: Boolean = true): IntArray {
        val ids = if (skipPinned) {
            clbDao.findUnpinnedIds()
        } else {
            clbDao.findAllIds()
        }
        clbDao.markAsDeleted(*ids)
        updateItemCount()
        return ids
    }

    suspend fun undoDelete(vararg ids: Int) {
        clbDao.undoDelete(*ids)
        updateItemCount()
    }

    suspend fun realDelete() {
        clbDao.realDelete()
    }

    suspend fun nukeTable() {
        withContext(coroutineContext) {
            clbDb.clearAllTables()
            updateItemCount()
        }
    }

    override fun onPrimaryClipChanged() {
        clipboardManager.primaryClip
            ?.let { ClipboardEntry.fromClipData(it, transformer) }
            ?.takeIf { it.text.isNotBlank() }
            ?.let { e ->
                launch {
                    mutex.withLock {
                        clbDao.find(e.text)?.let {
                            updateLastEntry(it.copy(timestamp = e.timestamp))
                            clbDao.updateTime(it.id, e.timestamp)
                            return@launch
                        }
                        val rowId = clbDao.insert(e)
                        removeOutdated()
                        updateItemCount()
                        // new entry can be deleted immediately if clipboard limit == 0
                        updateLastEntry(clbDao.get(rowId) ?: e)
                    }
                }
            }
    }

    private suspend fun removeOutdated() {
        val limit = limitPref.getValue()
        val unpinned = clbDao.getAllUnpinned()
        if (unpinned.size > limit) {
            // the last one we will keep
            val last = unpinned
                .sortedBy { it.id }
                .getOrNull(unpinned.size - limit)
            // delete all unpinned before that, or delete all when limit <= 0
            clbDao.markUnpinnedAsDeletedEarlierThan(last?.timestamp ?: System.currentTimeMillis())
        }
    }

}