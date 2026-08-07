/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
import timber.log.Timber

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao

    fun interface OnClipboardUpdateListener {
        fun onUpdate(entry: ClipboardEntry)
    }

    private val clipboardManager = appContext.clipboardManager

    private val mutex = Mutex()
    private val expiryMutex = Mutex()
    private var expiryTimer: Job? = null

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

    private val prefs = AppPrefs.getInstance().clipboard
    private val enabledPref = prefs.clipboardListening

    @Keep
    private val enabledListener = ManagedPreference.OnChangeListener<Boolean> { _, value ->
        if (value) {
            clipboardManager.addPrimaryClipChangedListener(this)
        } else {
            clipboardManager.removePrimaryClipChangedListener(this)
        }
    }

    private val limitPref = prefs.clipboardHistoryLimit

    @Keep
    private val limitListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        launch { removeOutdated() }
    }

    @Keep
    private val otpExpiryEnabledListener =
        ManagedPreference.OnChangeListener<Boolean> { _, _ ->
            launch { refreshExpiryPolicy(ClipboardCategory.Otp) }
        }

    @Keep
    private val otpExpiryDurationListener =
        ManagedPreference.OnChangeListener<Int> { _, _ ->
            launch { refreshExpiryPolicy(ClipboardCategory.Otp) }
        }

    @Keep
    private val trackingTokenExpiryEnabledListener =
        ManagedPreference.OnChangeListener<Boolean> { _, _ ->
            launch { refreshExpiryPolicy(ClipboardCategory.TrackingToken) }
        }

    @Keep
    private val trackingTokenExpiryDurationListener =
        ManagedPreference.OnChangeListener<Int> { _, _ ->
            launch { refreshExpiryPolicy(ClipboardCategory.TrackingToken) }
        }

    var lastEntry: ClipboardEntry? = null

    private fun updateLastEntry(entry: ClipboardEntry) {
        lastEntry = entry
        onUpdateListeners.forEach { it.onUpdate(entry) }
    }

    fun init(context: Context) {
        clbDb = Room
            .databaseBuilder(context, ClipboardDatabase::class.java, "clbdb")
            // allow wipe the database instead of crashing when downgrade
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        clbDao = clbDb.clipboardDao()
        enabledListener.onChange(enabledPref.key, enabledPref.getValue())
        enabledPref.registerOnChangeListener(enabledListener)
        limitListener.onChange(limitPref.key, limitPref.getValue())
        limitPref.registerOnChangeListener(limitListener)
        prefs.clipboardOtpAutoDelete.registerOnChangeListener(otpExpiryEnabledListener)
        prefs.clipboardOtpAutoDeleteMinutes.registerOnChangeListener(otpExpiryDurationListener)
        prefs.clipboardTrackingTokenAutoDelete.registerOnChangeListener(
            trackingTokenExpiryEnabledListener
        )
        prefs.clipboardTrackingTokenAutoDeleteHours.registerOnChangeListener(
            trackingTokenExpiryDurationListener
        )
        launch {
            updateItemCount()
            reclassifyOutdatedEntries()
            initializeExpiryMaintenance()
        }
    }

    suspend fun get(id: Int) = clbDao.get(id)

    suspend fun haveDeletable() = clbDao.haveDeletable()

    fun entries(filter: ClipboardEntryFilter) = when (filter.scope) {
        ClipboardEntryFilter.Scope.All -> clbDao.allEntries(filter.category)
        ClipboardEntryFilter.Scope.Favorites -> clbDao.favoriteEntries(filter.category)
    }

    suspend fun pin(id: Int) = expiryMutex.withLock {
        clbDao.updatePinStatus(id, true)
        scheduleNextExpiryLocked()
    }

    suspend fun unpin(id: Int) = clbDao.updatePinStatus(id, false)

    suspend fun favorite(id: Int) = expiryMutex.withLock {
        clbDao.updateFavoriteStatus(id, true)
        scheduleNextExpiryLocked()
    }

    suspend fun unfavorite(id: Int) = clbDao.updateFavoriteStatus(id, false)

    suspend fun updateText(id: Int, text: String) {
        val entry = clbDao.get(id) ?: return
        val analysis = ClipboardContentAnalyzer.analyze(text)
        val updated = entry.copy(
            text = text,
            category = analysis.category,
            classificationVersion = ClipboardContentAnalyzer.VERSION,
            expiresAt = expiryFor(
                analysis.category,
                System.currentTimeMillis(),
                entry.pinned,
                entry.favorite
            )
        )
        if (lastEntry?.id == id) updateLastEntry(updated)
        clbDao.updateTextAndClassification(
            id,
            text,
            analysis.category,
            ClipboardContentAnalyzer.VERSION,
            updated.expiresAt
        )
        refreshExpiryTimer()
    }

    suspend fun delete(id: Int) {
        clbDao.markAsDeleted(id)
        updateItemCount()
        refreshExpiryTimer()
    }

    suspend fun deleteAll(): IntArray {
        val ids = clbDao.findDeletableIds()
        clbDao.markAsDeleted(*ids)
        updateItemCount()
        refreshExpiryTimer()
        return ids
    }

    suspend fun undoDelete(vararg ids: Int) {
        clbDao.undoDelete(*ids)
        updateItemCount()
        refreshExpiryTimer()
    }

    suspend fun realDelete() {
        clbDao.realDelete()
    }

    suspend fun nukeTable() {
        withContext(coroutineContext) {
            clbDb.clearAllTables()
            updateItemCount()
        }
        refreshExpiryTimer()
    }

    private var lastClipTimestamp = -1L
    private var lastClipHash = 0

    override fun onPrimaryClipChanged() {
        val clip = clipboardManager.primaryClip ?: return
        /**
         * skip duplicate ClipData
         * https://developer.android.com/reference/android/content/ClipboardManager.OnPrimaryClipChangedListener#onPrimaryClipChanged()
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timestamp = clip.description.timestamp
            if (timestamp == lastClipTimestamp) return
            lastClipTimestamp = timestamp
        } else {
            val timestamp = System.currentTimeMillis()
            val hash = clip.hashCode()
            if (timestamp - lastClipTimestamp < 100L && hash == lastClipHash) return
            lastClipTimestamp = timestamp
            lastClipHash = hash
        }
        launch {
            mutex.withLock {
                val rawEntry = ClipboardEntry.fromClipData(clip, transformer) ?: return@withLock
                if (rawEntry.text.isBlank()) return@withLock
                val analysis = ClipboardContentAnalyzer.analyze(rawEntry.text)
                val entry = rawEntry.copy(
                    category = analysis.category,
                    classificationVersion = ClipboardContentAnalyzer.VERSION,
                    expiresAt = expiryFor(
                        analysis.category,
                        rawEntry.timestamp,
                        rawEntry.pinned,
                        rawEntry.favorite
                    )
                )
                try {
                    clbDao.find(entry.text, entry.sensitive)?.let {
                        val updated = it.copy(
                            timestamp = entry.timestamp,
                            category = entry.category,
                            classificationVersion = ClipboardContentAnalyzer.VERSION,
                            expiresAt = expiryFor(
                                entry.category,
                                entry.timestamp,
                                it.pinned,
                                it.favorite
                            )
                        )
                        updateLastEntry(updated)
                        clbDao.updateTimeAndClassification(
                            it.id,
                            entry.timestamp,
                            entry.category,
                            ClipboardContentAnalyzer.VERSION,
                            updated.expiresAt
                        )
                        refreshExpiryTimer()
                        return@withLock
                    }
                    val insertedEntry = clbDb.withTransaction {
                        val rowId = clbDao.insert(entry)
                        removeOutdated()
                        // new entry can be deleted immediately if clipboard limit == 0
                        clbDao.get(rowId) ?: entry
                    }
                    updateLastEntry(insertedEntry)
                    updateItemCount()
                    refreshExpiryTimer()
                } catch (exception: Exception) {
                    Timber.w("Failed to update clipboard database: $exception")
                    updateLastEntry(entry)
                }
            }
        }
    }

    private suspend fun removeOutdated() {
        val limit = limitPref.getValue()
        val unprotected = clbDao.getAllUnprotected()
        if (unprotected.size > limit) {
            // the last one we will keep
            val last = unprotected
                .sortedBy { it.timestamp }
                .getOrNull(unprotected.size - limit)
            // delete all unprotected before that, or delete all when limit <= 0
            clbDao.markUnprotectedAsDeletedEarlierThan(last?.timestamp ?: System.currentTimeMillis())
        }
    }

    private suspend fun reclassifyOutdatedEntries() {
        while (true) {
            val entries = clbDao.entriesToClassify(ClipboardContentAnalyzer.VERSION, 100)
            if (entries.isEmpty()) return
            clbDb.withTransaction {
                entries.forEach { entry ->
                    val analysis = ClipboardContentAnalyzer.analyze(entry.text)
                    clbDao.updateClassification(
                        entry.id,
                        analysis.category,
                        ClipboardContentAnalyzer.VERSION
                    )
                }
            }
        }
    }

    suspend fun cleanupExpired() {
        expiryMutex.withLock {
            deleteExpiredLocked()
            scheduleNextExpiryLocked()
        }
    }

    private fun expirySettings() = ClipboardExpiryPolicy.Settings(
        otpEnabled = prefs.clipboardOtpAutoDelete.getValue(),
        otpMinutes = prefs.clipboardOtpAutoDeleteMinutes.getValue(),
        trackingTokenEnabled = prefs.clipboardTrackingTokenAutoDelete.getValue(),
        trackingTokenHours = prefs.clipboardTrackingTokenAutoDeleteHours.getValue()
    )

    private fun expiryFor(
        category: ClipboardCategory,
        copiedAt: Long,
        pinned: Boolean,
        favorite: Boolean
    ) = ClipboardExpiryPolicy.expiresAt(
        category,
        copiedAt,
        pinned,
        favorite,
        expirySettings()
    )

    private fun enabledExpiryCategories(
        settings: ClipboardExpiryPolicy.Settings = expirySettings()
    ) = buildList {
        if (settings.otpEnabled) add(ClipboardCategory.Otp)
        if (settings.trackingTokenEnabled) add(ClipboardCategory.TrackingToken)
    }

    private suspend fun initializeExpiryMaintenance() {
        expiryMutex.withLock {
            val settings = expirySettings()
            if (!settings.otpEnabled) clbDao.clearExpiry(ClipboardCategory.Otp)
            if (!settings.trackingTokenEnabled) {
                clbDao.clearExpiry(ClipboardCategory.TrackingToken)
            }
            deleteExpiredLocked(settings)
            scheduleNextExpiryLocked(settings)
        }
    }

    private suspend fun refreshExpiryPolicy(category: ClipboardCategory) {
        expiryMutex.withLock {
            val settings = expirySettings()
            val expiresAt = ClipboardExpiryPolicy.expiresAt(
                category,
                System.currentTimeMillis(),
                pinned = false,
                favorite = false,
                settings
            )
            if (expiresAt == null) {
                clbDao.clearExpiry(category)
            } else {
                clbDao.resetExpiry(category, expiresAt)
            }
            deleteExpiredLocked(settings)
            scheduleNextExpiryLocked(settings)
        }
    }

    private suspend fun refreshExpiryTimer() {
        expiryMutex.withLock {
            scheduleNextExpiryLocked()
        }
    }

    private suspend fun deleteExpiredLocked(
        settings: ClipboardExpiryPolicy.Settings = expirySettings()
    ) {
        val categories = enabledExpiryCategories(settings)
        if (categories.isEmpty()) return
        if (clbDao.deleteExpired(categories, System.currentTimeMillis()) > 0) {
            updateItemCount()
        }
    }

    private suspend fun scheduleNextExpiryLocked(
        settings: ClipboardExpiryPolicy.Settings = expirySettings()
    ) {
        expiryTimer?.cancel()
        expiryTimer = null
        val categories = enabledExpiryCategories(settings)
        if (categories.isEmpty()) return
        val expiresAt = clbDao.nearestExpiry(categories) ?: return
        val waitMillis = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
        expiryTimer = launch {
            delay(waitMillis)
            expiryMutex.withLock {
                expiryTimer = null
                deleteExpiredLocked()
                scheduleNextExpiryLocked()
            }
        }
    }

}
