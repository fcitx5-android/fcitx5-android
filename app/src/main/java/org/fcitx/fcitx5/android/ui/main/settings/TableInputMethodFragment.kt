package org.fcitx.fcitx5.android.ui.main.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.table.TableBasedInputMethod
import org.fcitx.fcitx5.android.data.table.TableManager
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.*
import splitties.resources.styledDrawable
import splitties.systemservices.notificationManager
import splitties.views.imageDrawable
import java.io.File

class TableInputMethodFragment : Fragment(), OnItemChangedListener<TableBasedInputMethod> {

    private val viewModel: MainViewModel by activityViewModels()

    private val entries: List<TableBasedInputMethod>
        get() = ui.entries

    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var launcher: ActivityResultLauncher<String>

    private val dustman = NaiveDustman<TableBasedInputMethod>().apply {
        onDirty = {
            viewModel.enableToolbarSaveButton { reloadConfig() }
        }
        onClean = {
            viewModel.disableToolbarSaveButton()
        }
    }

    private val ui: BaseDynamicListUi<TableBasedInputMethod> by lazy {
        object : BaseDynamicListUi<TableBasedInputMethod>(
            requireContext(),
            Mode.Custom(),
            TableManager.inputMethods(),
            initSettingsButton = {
                visibility = if (it.tableFileExists) View.GONE else View.VISIBLE
                imageDrawable = styledDrawable(android.R.attr.alertDialogIcon)
                setOnClickListener { _: View ->
                    if (it.tableFileExists) return@setOnClickListener
                    lifecycleScope.launch {
                        errorDialog(
                            requireContext(),
                            getString(R.string.table_file_does_not_exist_title),
                            getString(R.string.table_file_does_not_exist_message, it.tableFileName)
                        )
                    }
                }
            }
        ) {
            init {
                addTouchCallback()
                fab.setOnClickListener {
                    launcher.launch("application/zip")
                }
                enableUndo = false
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: TableBasedInputMethod): String = x.name
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createNotificationChannel()
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(getString(R.string.table_im))
        registerLauncher()
        ui.addOnItemChangedListener(this)
        resetDustman()
        return ui.root
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getText(R.string.table_im),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_ID }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null)
                importFromUri(uri)
        }
    }

    private fun importFromUri(uri: Uri) =
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val importId = IMPORT_ID++
            runCatching {
                val fileName = uri.queryFileName(contentResolver)
                fileName.filter { it.endsWith(".zip") }.orNull()
                    ?: errorArg(R.string.exception_table_im_filename, fileName.getOrElse { "" })
                val file = File(fileName.orNull()!!)
                val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.table_im))
                    .setContentText("${getString(R.string.importing)} ${file.nameWithoutExtension}")
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.build().let { notificationManager.notify(importId, it) }
                contentResolver.openInputStream(uri)
            }.bindOnNotNull {
                TableManager.importTableBasedIM(it)
            }?.onFailure {
                importErrorDialog(it.localizedMessage ?: it.stackTraceToString())
            }?.onSuccess {
                launch(Dispatchers.Main) {
                    ui.addItem(item = it)
                }
            }
            notificationManager.cancel(importId)
        }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    private fun reloadConfig() {
        if (!dustman.dirty)
            return
        resetDustman()
        lifecycleScope.launch {
            viewModel.fcitx.runOnReady { reloadConfig() }
        }
    }

    private fun resetDustman() {
        dustman.reset((entries.associateBy { it.name }))
    }

    override fun onItemAdded(idx: Int, item: TableBasedInputMethod) {
        dustman.addOrUpdate(item.name, item)
    }

    override fun onItemRemoved(idx: Int, item: TableBasedInputMethod) {
        item.delete()
        dustman.remove(item.name)
    }

    override fun onItemUpdated(idx: Int, old: TableBasedInputMethod, new: TableBasedInputMethod) {
        dustman.addOrUpdate(new.name, new)
    }

    override fun onPause() {
        reloadConfig()
        super.onPause()
    }

    companion object {
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "table_dict"
    }
}