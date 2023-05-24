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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.data.table.TableBasedInputMethod
import org.fcitx.fcitx5.android.data.table.TableManager
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.bindOnNotNull
import org.fcitx.fcitx5.android.utils.errorArg
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.notificationManager
import org.fcitx.fcitx5.android.utils.queryFileName
import splitties.resources.styledDrawable
import splitties.views.imageDrawable

class TableInputMethodFragment : Fragment(), OnItemChangedListener<TableBasedInputMethod> {

    private val viewModel: MainViewModel by activityViewModels()

    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var zipLauncher: ActivityResultLauncher<String>
    private lateinit var confLauncher: ActivityResultLauncher<String>
    private lateinit var dictLauncher: ActivityResultLauncher<String>

    private var confUri: Uri? = null
    private var dictUri: Uri? = null
    private var filesSelectionDialog: AlertDialog? = null

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
                    showImportDialog()
                }
                enableUndo = false
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: TableBasedInputMethod): String = x.name
        }
    }

    private val filesSelectionUi by lazy {
        TableFilesSelectionUi(requireContext()).apply {
            conf.root.setOnClickListener {
                confLauncher.launch("*/*")
            }
            dict.root.setOnClickListener {
                dictLauncher.launch("*/*")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        createNotificationChannel()
        registerLauncher()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        resetDustman()
        ui.addOnItemChangedListener(this)
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
        zipLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) importZipFromUri(uri)
        }
        confLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) prepareConfFromUri(uri)
        }
        dictLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) prepareDictFromUri(uri)
        }
    }

    private fun showImportDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.import_table)
            .setItems(
                arrayOf(
                    getString(R.string.table_from_zip),
                    getString(R.string.table_select_files)
                )
            ) { _, which ->
                when (which) {
                    0 -> zipLauncher.launch("application/zip")
                    1 -> showFilesSelectionDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFilesSelectionDialog() {
        filesSelectionDialog?.dismiss()
        confUri = null
        dictUri = null
        filesSelectionUi.reset()
        filesSelectionDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.import_table)
            .setView(filesSelectionUi.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                (filesSelectionUi.root.parent as? ViewGroup)?.removeView(filesSelectionUi.root)
            }
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    // override default button handler to prevent dialog close on click
                    setOnClickListener {
                        importConfAndDictUri()
                    }
                    isEnabled = false
                }
            }
    }

    private fun updateFilesSelectionDialogButton() {
        filesSelectionDialog?.apply {
            getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = (confUri != null && dictUri != null)
        }
    }

    private fun dismissFilesSelectionDialog() {
        filesSelectionDialog?.dismiss()
        filesSelectionDialog = null
    }

    private fun importZipFromUri(uri: Uri) {
        val nm = notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val importId = IMPORT_ID++
            runCatching {
                val fileName = uri.queryFileName(contentResolver).orNull() ?: return@launch
                if (!fileName.endsWith(".zip")) {
                    errorArg(R.string.exception_table_im_filename, fileName)
                }
                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.table_im))
                    .setContentText("${getString(R.string.importing)} $fileName")
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build().let { nm.notify(importId, it) }
                contentResolver.openInputStream(uri)
            }.bindOnNotNull {
                TableManager.importFromZip(it)
            }?.onFailure {
                importErrorDialog(it.localizedMessage ?: it.stackTraceToString())
            }?.onSuccess {
                launch(Dispatchers.Main) {
                    ui.addItem(item = it)
                }
            }
            nm.cancel(importId)
        }
    }

    private fun prepareConfFromUri(uri: Uri) {
        lifecycleScope.launch {
            val fileName = uri.queryFileName(contentResolver).orNull() ?: return@launch
            if (!fileName.removeSuffix(".in").endsWith(".conf")) {
                importErrorDialog(getString(R.string.exception_table_conf_filename, fileName))
                return@launch
            }
            confUri = uri
            filesSelectionUi.conf.summary.text = fileName
            updateFilesSelectionDialogButton()
        }
    }

    private fun prepareDictFromUri(uri: Uri) {
        lifecycleScope.launch {
            val fileName = uri.queryFileName(contentResolver).orNull() ?: return@launch
            if (!fileName.endsWith(".dict") && !fileName.endsWith(".txt")) {
                importErrorDialog(getString(R.string.exception_table_dict_filename, fileName))
                return@launch
            }
            dictUri = uri
            filesSelectionUi.dict.summary.text = fileName
            updateFilesSelectionDialogButton()
        }
    }

    private fun importConfAndDictUri() {
        val nm = notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            if (confUri == null || dictUri == null) {
                importErrorDialog(getString(R.string.exception_table_import_both_files))
                return@launch
            }
            val importId = IMPORT_ID++
            runCatching {
                val confName = confUri?.queryFileName(contentResolver)?.orNull() ?: return@launch
                val dictName = dictUri?.queryFileName(contentResolver)?.orNull() ?: return@launch
                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.table_im))
                    .setContentText("${getString(R.string.importing)} $confName")
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build().let { nm.notify(importId, it) }
                val confStream = contentResolver.openInputStream(confUri!!) ?: return@launch
                val dictStream = contentResolver.openInputStream(dictUri!!) ?: return@launch
                withContext(Dispatchers.Main) {
                    filesSelectionDialog?.apply {
                        getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    }
                }
                TableManager.importFromConfAndDict(confName, confStream, dictName, dictStream)
                    .getOrThrow()
            }.onFailure {
                importErrorDialog(it.localizedMessage ?: it.stackTraceToString())
            }.onSuccess {
                launch(Dispatchers.Main) {
                    dismissFilesSelectionDialog()
                    ui.addItem(item = it)
                }
            }
            nm.cancel(importId)
            withContext(Dispatchers.Main) {
                filesSelectionDialog?.apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
            }
        }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    private fun reloadConfig() {
        if (!dustman.dirty) return
        resetDustman()
        lifecycleScope.launchOnFcitxReady(viewModel.fcitx) {
            it.reloadConfig()
        }
    }

    private fun resetDustman() {
        dustman.reset(ui.entries.associateBy { it.name })
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

    override fun onDestroy() {
        // prevent dustman calling viewModel after Fragment detached
        ui.removeItemChangedListener()
        super.onDestroy()
    }

    companion object {
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "table_dict"
    }
}