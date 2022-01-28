package me.rocka.fcitx5test.data.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.data.clipboard.db.ClipboardDao
import me.rocka.fcitx5test.data.clipboard.db.ClipboardDatabase
import me.rocka.fcitx5test.data.clipboard.db.ClipboardEntry
import me.rocka.fcitx5test.utils.UTF8Utils
import splitties.systemservices.clipboardManager

object ClipboardManager : ClipboardManager.OnPrimaryClipChangedListener,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private lateinit var clbDb: ClipboardDatabase
    private lateinit var clbDao: ClipboardDao

    private val enabled
        get() = Prefs.getInstance().clipboard

    private val limit
        get() = Prefs.getInstance().clipboardHistoryLimit

    fun init(context: Context) {
        clipboardManager.addPrimaryClipChangedListener(this)
        clbDb = Room
            .databaseBuilder(context, ClipboardDatabase::class.java, "clbdb")
            .build()
        clbDao = clbDb.clipboardDao()
    }

    suspend fun getAll() = withContext(coroutineContext) { clbDao.getAll() }

    fun pin(id: Int) {
        launch { clbDao.updatePinStatus(id, true) }
    }

    fun unpin(id: Int) {
        launch { clbDao.updatePinStatus(id, false) }
    }

    override fun onPrimaryClipChanged() {
        if (!(enabled && this::clbDao.isInitialized))
            return
        clipboardManager
            .primaryClip
            ?.let { ClipboardEntry.fromClipData(it) }
            ?.takeIf { UTF8Utils.instance.validateUTF8(it.text) }
            ?.let {
                launch {
                    clbDao.insertAll(it)
                    removeOutdated()
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
            // delete all before that
            clbDao.deleteIdLessThan(last.id)
        }
    }

}