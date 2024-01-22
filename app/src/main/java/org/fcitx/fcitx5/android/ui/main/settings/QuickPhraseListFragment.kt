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
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.reloadQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.BuiltinQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.CustomQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseManager
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.materialTextInput
import org.fcitx.fcitx5.android.utils.notificationManager
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.str
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.imageDrawable
import splitties.views.setPaddingDp
import java.util.concurrent.atomic.AtomicBoolean

class QuickPhraseListFragment : Fragment(), OnItemChangedListener<QuickPhrase> {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var launcher: ActivityResultLauncher<String>

    private val busy: AtomicBoolean = AtomicBoolean(false)

    private val dustman = NaiveDustman<Boolean>()

    private var uiInitialized = false

    private val ui: BaseDynamicListUi<QuickPhrase> by lazy {
        object : BaseDynamicListUi<QuickPhrase>(
            requireContext(),
            Mode.Custom(),
            QuickPhraseManager.listQuickPhrase(),
            initCheckBox = { entry ->
                setOnCheckedChangeListener(null)
                isEnabled = true
                isChecked = entry.isEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) entry.enable() else entry.disable()
                    ui.updateItem(ui.indexItem(entry), entry)
                }
            },
            initSettingsButton = { entry ->
                visibility = if (!entry.isEnabled) View.GONE else View.VISIBLE
                fun edit() {
                    findNavController().navigate(
                        R.id.action_quickPhraseListFragment_to_quickPhraseEditFragment,
                        bundleOf(QuickPhraseEditFragment.ARG to entry)
                    )
                    parentFragmentManager.setFragmentResultListener(
                        QuickPhraseEditFragment.RESULT,
                        this@QuickPhraseListFragment
                    ) { _, _ ->
                        ui.updateItem(ui.indexItem(entry), entry)
                        // editor changed file content
                        dustman.forceDirty()
                    }
                }

                var icon = R.drawable.ic_baseline_settings_24
                when (entry) {
                    is BuiltinQuickPhrase -> {
                        if (entry.override != null) {
                            icon = R.drawable.ic_baseline_expand_more_24
                            setOnClickListener {
                                PopupMenu(requireContext(), this).apply {
                                    menu.add(getString(R.string.edit)).setOnMenuItemClickListener {
                                        edit()
                                        true
                                    }
                                    menu.add(getString(R.string.reset)).setOnMenuItemClickListener {
                                        entry.deleteOverride()
                                        ui.updateItem(ui.indexItem(entry), entry)
                                        // not sure if the content changes
                                        dustman.forceDirty()
                                        true
                                    }
                                    show()
                                }
                            }
                        } else {
                            icon = R.drawable.ic_baseline_edit_24
                            setOnClickListener {
                                edit()
                            }
                        }

                    }
                    is CustomQuickPhrase -> {
                        icon = R.drawable.ic_baseline_edit_24
                        setOnClickListener {
                            edit()
                        }
                    }
                }
                imageDrawable = drawable(icon)!!.apply {
                    setTint(styledColor(android.R.attr.colorControlNormal))
                }
            }
        ) {
            init {
                enableUndo = false
                shouldShowFab = true
                fab.setOnClickListener {
                    // TODO use expandable fab instead
                    showImportOrCreateDialog()
                }
                setViewModel(viewModel)
                // Builtin quick phrase shouldn't be removed
                // But it can be disabled
                removable = { e -> e !is BuiltinQuickPhrase }
                addTouchCallback()
            }

            private fun showImportOrCreateDialog() {
                val actions = arrayOf(
                    getString(R.string.import_from_file),
                    getString(R.string.create_new)
                )
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.quickphrase_editor)
                    .setItems(actions) { _, i ->
                        when (i) {
                            0 -> launcher.launch("*/*")
                            1 -> showCreateQuickPhraseDialog()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            private fun showCreateQuickPhraseDialog() {
                val (inputLayout, editText) = materialTextInput {
                    setHint(R.string.name)
                }
                val layout = verticalLayout {
                    setPaddingDp(20, 10, 20, 0)
                    add(inputLayout, lParams(matchParent))
                }
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.create_new)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener onClick@{
                    val name = editText.str.trim()
                    if (name.isBlank()) {
                        editText.error =
                            getString(R.string._cannot_be_empty, getString(R.string.name))
                        editText.requestFocus()
                        return@onClick
                    } else {
                        editText.error = null
                    }
                    ui.addItem(item = QuickPhraseManager.newEmpty(name))
                    dialog.dismiss()
                }
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: QuickPhrase): String = x.name

        }.also {
            uiInitialized = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getText(R.string.quickphrase_editor),
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

    private fun importFromUri(uri: Uri) {
        val ctx = requireContext()
        val cr = ctx.contentResolver
        val nm = ctx.notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val id = IMPORT_ID++
            val fileName = cr.queryFileName(uri) ?: return@launch
            val extName = fileName.substringAfterLast('.')
            if (extName != QuickPhrase.EXT) {
                importErrorDialog(getString(R.string.exception_quickphrase_filename, fileName))
                return@launch
            }
            val entryName = fileName.substringBeforeLast('.')
            if (ui.entries.any { it.name == entryName }) {
                importErrorDialog(getString(R.string.quickphrase_already_exists))
                return@launch
            }
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_format_quote_24)
                .setContentTitle(getString(R.string.quickphrase_editor))
                .setContentText("${getString(R.string.importing)} $entryName")
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build().let { nm.notify(id, it) }
            try {
                val inputStream = cr.openInputStream(uri)!!
                val imported = QuickPhraseManager.importFromInputStream(inputStream, fileName)
                    .getOrThrow()
                withContext(Dispatchers.Main) {
                    ui.addItem(item = imported)
                }
            } catch (e: Exception) {
                importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
            }
            nm.cancel(id)
        }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    private fun reloadQuickPhrase() {
        if (!dustman.dirty) return
        resetDustman()
        // save the reference to NotificationManager, in case we need to cancel notification
        // after Fragment detached
        val nm = notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            if (busy.compareAndSet(false, true)) {
                val id = RELOAD_ID++
                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.quickphrase_editor))
                    .setContentText(getString(R.string.reloading))
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build().let { nm.notify(id, it) }
                viewModel.fcitx.runOnReady {
                    reloadQuickPhrase()
                }
                nm.cancel(id)
                busy.set(false)
            }
        }
    }

    private fun resetDustman() {
        dustman.reset(ui.entries.associate { it.name to it.isEnabled })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createNotificationChannel()
        registerLauncher()
        ui.addOnItemChangedListener(this)
        resetDustman()
        return ui.root
    }

    override fun onItemAdded(idx: Int, item: QuickPhrase) {
        dustman.addOrUpdate(item.name, item.isEnabled)
    }

    override fun onItemRemoved(idx: Int, item: QuickPhrase) {
        item.file.delete()
        dustman.remove(item.name)
    }

    override fun onItemRemovedBatch(indexed: List<Pair<Int, QuickPhrase>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: QuickPhrase, new: QuickPhrase) {
        dustman.addOrUpdate(new.name, new.isEnabled)
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
        reloadQuickPhrase()
        viewModel.disableToolbarEditButton()
        if (uiInitialized) {
            ui.exitMultiSelect()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (uiInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

    companion object {
        private var RELOAD_ID = 0
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "quickphrase"
    }
}