package org.fcitx.fcitx5.android.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.utils.UTF8Utils
import org.fcitx.fcitx5.android.utils.WeakHashSet
import splitties.systemservices.clipboardManager

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao

    fun interface OnClipboardUpdateListener {
        fun onUpdate(text: String)
    }

    private val mutex = Mutex()

    var itemCount: Int = 0
        private set

    private suspend fun updateItemCount() {
        itemCount = clbDao.itemCount()
    }

    private val onUpdateListeners = WeakHashSet<OnClipboardUpdateListener>()

    fun addOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.add(listener)
    }

    fun removeOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.remove(listener)
    }

    private val enabledPref = AppPrefs.getInstance().clipboard.clipboardListening
    private val enabledListener = ManagedPreference.OnChangeListener<Boolean> { _, value ->
        if (value) {
            clipboardManager.addPrimaryClipChangedListener(this)
        } else {
            clipboardManager.removePrimaryClipChangedListener(this)
        }
    }

    private val limitPref = AppPrefs.getInstance().clipboard.clipboardHistoryLimit
    private val limitListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        launch { removeOutdated() }
    }

    var lastEntry: ClipboardEntry? = null
    var lastEntryTimestamp: Long = -1L

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

    suspend fun getAll() = clbDao.getAll()

    suspend fun pin(id: Int) = clbDao.updatePinStatus(id, true)

    suspend fun unpin(id: Int) = clbDao.updatePinStatus(id, false)

    suspend fun updateText(id: Int, text: String) = clbDao.updateText(id, text)

    suspend fun delete(id: Int) {
        clbDao.delete(id)
        updateItemCount()
    }

    suspend fun deleteAll(skipPinned: Boolean = true) {
        if (skipPinned)
            clbDao.deleteAllUnpinned()
        else
            clbDao.deleteAll()
        updateItemCount()
    }

    suspend fun nukeTable() {
        withContext(coroutineContext) {
            clbDb.clearAllTables()
            updateItemCount()
        }
    }

    override fun onPrimaryClipChanged() {
        clipboardManager.primaryClip
            ?.let { ClipboardEntry.fromClipData(it) }
            ?.takeIf { it.text.isNotBlank() && UTF8Utils.instance.validateUTF8(it.text) }
            ?.let { e ->
                launch {
                    mutex.withLock {
                        val all = clbDao.getAll()
                        var pinned = false
                        all.find { e.text == it.text }?.let {
                            clbDao.delete(it.id)
                            pinned = it.pinned
                        }
                        val rowId = clbDao.insert(e.copy(pinned = pinned))
                        removeOutdated()
                        updateItemCount()
                        clbDao.get(rowId)?.let { newEntry ->
                            lastEntry = newEntry
                            lastEntryTimestamp = System.currentTimeMillis()
                            onUpdateListeners.forEach { listener ->
                                listener.onUpdate(newEntry.text)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun removeOutdated() {
        val limit = limitPref.getValue()
        val unpinned = clbDao.getAll().filter { !it.pinned }
        if (unpinned.size > limit) {
            // the last one we will keep
            val last = unpinned
                .sortedBy { it.id }
                .getOrNull(unpinned.size - limit)
            // delete all unpinned before that, or delete all when limit <= 0
            clbDao.deleteUnpinnedIdLessThan(last?.id ?: Int.MAX_VALUE)
        }
    }

}