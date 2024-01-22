/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.table.TableBasedInputMethod
import org.fcitx.fcitx5.android.data.table.TableManager
import org.fcitx.fcitx5.android.data.table.dict.Dictionary
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.notificationManager
import org.fcitx.fcitx5.android.utils.onPositiveButtonClick
import org.fcitx.fcitx5.android.utils.positiveButton
import org.fcitx.fcitx5.android.utils.queryFileName
import splitties.resources.drawable
import splitties.resources.styledDrawable
import splitties.views.imageDrawable

class TableInputMethodFragment : Fragment(), OnItemChangedListener<TableBasedInputMethod> {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var zipLauncher: ActivityResultLauncher<String>
    private lateinit var confLauncher: ActivityResultLauncher<String>
    private lateinit var dictLauncher: ActivityResultLauncher<String>
    private lateinit var replaceLauncher: ActivityResultLauncher<String>

    private var confUri: Uri? = null
    private var dictUri: Uri? = null
    private var filesSelectionDialog: AlertDialog? = null
    private var tableToReplace: TableBasedInputMethod? = null

    private val dustman = NaiveDustman<TableBasedInputMethod>()

    private var uiInitialized = false

    private val ui: BaseDynamicListUi<TableBasedInputMethod> by lazy {
        object : BaseDynamicListUi<TableBasedInputMethod>(
            requireContext(),
            Mode.Custom(),
            TableManager.inputMethods(),
            initSettingsButton = {
                visibility = View.VISIBLE
                imageDrawable =
                    if (it.tableFileExists) drawable(R.drawable.ic_baseline_edit_24)
                    else styledDrawable(android.R.attr.alertDialogIcon)
                setOnClickListener { _ ->
                    tableToReplace = it
                    lifecycleScope.launch {
                        if (it.tableFileExists) {
                            showReplaceTableDialog(it)
                        } else {
                            showMissingTableDictDialog(it)
                        }
                    }
                }
            }
        ) {
            init {
                addTouchCallback()
                shouldShowFab = true
                fab.setOnClickListener {
                    showImportDialog()
                }
                enableUndo = false
                setViewModel(viewModel)
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: TableBasedInputMethod): String = x.name
        }.also {
            uiInitialized = true
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
        replaceLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) replaceDictFromUri(uri)
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
            .show()
            .onPositiveButtonClick {
                positiveButton.isEnabled = false
                importConfAndDictUri()
                false
            }
    }

    private fun updateFilesSelectionDialogButton(importing: Boolean = false) {
        filesSelectionDialog?.apply {
            positiveButton.isEnabled =
                if (importing) false else (confUri != null && dictUri != null)
        }
    }

    private fun dismissFilesSelectionDialog() {
        filesSelectionDialog?.dismiss()
        filesSelectionDialog = null
    }

    private fun importZipFromUri(uri: Uri) {
        val ctx = requireContext()
        val cr = ctx.contentResolver
        val nm = ctx.notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val importId = IMPORT_ID++
            val fileName = cr.queryFileName(uri) ?: return@launch
            if (!fileName.endsWith(".zip")) {
                importErrorDialog(getString(R.string.exception_table_im_filename, fileName))
                return@launch
            }
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                .setContentTitle(getString(R.string.table_im))
                .setContentText("${getString(R.string.importing)} $fileName")
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build().let { nm.notify(importId, it) }
            try {
                val inputStream = cr.openInputStream(uri)!!
                val imported = TableManager.importFromZip(inputStream).getOrThrow()
                withContext(Dispatchers.Main) {
                    ui.addItem(item = imported)
                }
            } catch (e: Exception) {
                importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
            }
            nm.cancel(importId)
        }
    }

    private fun prepareConfFromUri(uri: Uri) {
        val ctx = requireContext()
        lifecycleScope.launch {
            val fileName = ctx.contentResolver.queryFileName(uri) ?: ""
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
        val ctx = requireContext()
        lifecycleScope.launch {
            val fileName = ctx.contentResolver.queryFileName(uri) ?: ""
            if (Dictionary.Type.fromFileName(fileName) == null) {
                importErrorDialog(getString(R.string.exception_table_dict_filename, fileName))
                return@launch
            }
            dictUri = uri
            filesSelectionUi.dict.summary.text = fileName
            updateFilesSelectionDialogButton()
        }
    }

    private fun importConfAndDictUri() {
        val ctx = requireContext()
        val cr = ctx.contentResolver
        val nm = ctx.notificationManager
        val confUri = this@TableInputMethodFragment.confUri
        val dictUri = this@TableInputMethodFragment.dictUri
        if (confUri == null || dictUri == null) {
            lifecycleScope.launch {
                importErrorDialog(getString(R.string.exception_table_import_both_files))
            }
            return
        }
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val importId = IMPORT_ID++
            val confName = cr.queryFileName(confUri) ?: return@launch
            val dictName = cr.queryFileName(dictUri) ?: return@launch
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                .setContentTitle(getString(R.string.table_im))
                .setContentText("${getString(R.string.importing)} $confName")
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build().let { nm.notify(importId, it) }
            try {
                val confStream = cr.openInputStream(confUri)!!
                val dictStream = cr.openInputStream(dictUri)!!
                withContext(Dispatchers.Main) {
                    updateFilesSelectionDialogButton(importing = true)
                }
                val imported =
                    TableManager.importFromConfAndDict(confName, confStream, dictName, dictStream)
                        .getOrThrow()
                withContext(Dispatchers.Main) {
                    dismissFilesSelectionDialog()
                    ui.addItem(item = imported)
                }
            } catch (e: Exception) {
                importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
            }
            nm.cancel(importId)
            withContext(Dispatchers.Main) {
                updateFilesSelectionDialogButton(importing = false)
            }
        }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    private fun replaceDictFromUri(uri: Uri) {
        val ctx = requireContext()
        val cr = ctx.contentResolver
        val nm = ctx.notificationManager
        val im = tableToReplace ?: return
        tableToReplace = null
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val importId = IMPORT_ID++
            val dictName = cr.queryFileName(uri) ?: return@launch
            if (Dictionary.Type.fromFileName(dictName) == null) {
                importErrorDialog(getString(R.string.exception_table_dict_filename, dictName))
                return@launch
            }
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                .setContentTitle(getString(R.string.table_im))
                .setContentText("${getString(R.string.importing)} $dictName")
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build().let { nm.notify(importId, it) }
            try {
                val dictStream = cr.openInputStream(uri)!!
                im.table = TableManager.replaceTableDict(im, dictName, dictStream).getOrThrow()
                withContext(Dispatchers.Main) {
                    ui.updateItem(ui.indexItem(im), im)
                }
                dustman.forceDirty()
            } catch (e: Exception) {
                importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
            }
            nm.cancel(importId)
        }
    }

    private fun showReplaceTableDialog(im: TableBasedInputMethod) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_table)
            .setMessage(getString(R.string.table_dict_replace_message, im.tableFileName))
            .setNeutralButton(R.string.table_file_placeholder) { _, _ ->
                replaceLauncher.launch("*/*")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showMissingTableDictDialog(im: TableBasedInputMethod) {
        AlertDialog.Builder(requireContext())
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(R.string.table_file_does_not_exist_title)
            .setMessage(getString(R.string.table_file_does_not_exist_message, im.tableFileName))
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.table_file_placeholder) { _, _ ->
                replaceLauncher.launch("*/*")
            }
            .show()
    }

    private fun reloadConfig() {
        if (!dustman.dirty) return
        resetDustman()
        FcitxDaemon.restartFcitx()
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

    override fun onItemRemovedBatch(indexed: List<Pair<Int, TableBasedInputMethod>>) {
        batchRemove(indexed)
    }

    override fun onStart() {
        super.onStart()
        if (uiInitialized) {
            viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
                ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
            }
        }
    }

    override fun onStop() {
        reloadConfig()
        viewModel.disableToolbarEditButton()
        if (uiInitialized) {
            ui.exitMultiSelect()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (uiInitialized) {
            // prevent dustman calling viewModel after Fragment detached
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

    companion object {
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "table_dict"
    }
}