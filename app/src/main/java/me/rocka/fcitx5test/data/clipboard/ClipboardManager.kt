package me.rocka.fcitx5test.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.data.clipboard.db.ClipboardDao
import me.rocka.fcitx5test.data.clipboard.db.ClipboardDatabase
import me.rocka.fcitx5test.data.clipboard.db.ClipboardEntry
import me.rocka.fcitx5test.utils.UTF8Utils
import me.rocka.fcitx5test.utils.WeakHashSet
import splitties.systemservices.clipboardManager
import timber.log.Timber

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao

    fun interface OnClipboardUpdateListener {
        suspend fun onUpdate(text: String)
    }

    private val onUpdateListeners = WeakHashSet<OnClipboardUpdateListener>()

    fun addOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.add(listener)
    }

    fun removeOnUpdateListener(listener: OnClipboardUpdateListener) {
        onUpdateListeners.remove(listener)
    }

    private val enabled by Prefs.getInstance().clipboardListening

    private val limit by Prefs.getInstance().clipboardHistoryLimit

    fun init(context: Context) {
        clipboardManager.addPrimaryClipChangedListener(this)
        clbDb = Room
            .databaseBuilder(context, ClipboardDatabase::class.java, "clbdb")
            .build()
        clbDao = clbDb.clipboardDao()
    }

    suspend fun getAll() = clbDao.getAll()

    suspend fun pin(id: Int) = clbDao.updatePinStatus(id, true)

    suspend fun unpin(id: Int) = clbDao.updatePinStatus(id, false)

    suspend fun delete(id: Int) = clbDao.delete(id)

    suspend fun deleteAll() = clbDao.deleteAll()

    override fun onPrimaryClipChanged() {
        if (!(enabled && this::clbDao.isInitialized))
            return
        clipboardManager
            .primaryClip
            ?.let { ClipboardEntry.fromClipData(it) }
            ?.takeIf { it.text.isNotBlank() && UTF8Utils.instance.validateUTF8(it.text) }
            ?.let { e ->
                Timber.d("Accept $e")
                launch {
                    val all = clbDao.getAll()
                    var pinned = false
                    all.find { e.text == it.text }?.let {
                        clbDao.delete(it.id)
                        pinned = it.pinned
                    }
                    clbDao.insertAll(e.copy(pinned = pinned))
                    removeOutdated()
                    onUpdateListeners.forEach { listener ->
                        listener.onUpdate(e.text)
                    }
                }
            }

    }

    private suspend fun removeOutdated() {
        val all = clbDao.getAll()
        if (all.size > limit) {
            // the last one we will keep
            val last = all
                .map {
                    // pinned one should always be considered as the latest
                    if (it.pinned) it.copy(id = Int.MAX_VALUE)
                    else it
                }
                .sortedBy { it.id }[all.size - limit]
            // delete all unpinned before that
            clbDao.deleteUnpinnedIdLessThan(last.id)
        }
    }

}