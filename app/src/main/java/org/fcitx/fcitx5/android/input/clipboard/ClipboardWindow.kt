/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import androidx.core.content.edit
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.SnackbarContentLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.clipboardsync.MainService
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.BooleanKey.ClipboardDbEmpty
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.BooleanKey.ClipboardListeningEnabled
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.AddMore
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.EnableListening
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.Normal
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.TransitionEvent.ClipboardDbUpdated
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.TransitionEvent.ClipboardListeningUpdated
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.ClipboardSourceDeletionTarget
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.styledColorOrDefault
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.core.withTheme

class ClipboardWindow(
    private val initialCategory: ClipboardCategory? = null
) : InputWindow.ExtendedInputWindow<ClipboardWindow>() {
    companion object {
        private const val LAST_OPENED_CATEGORY_KEY = "clipboard_last_opened_category"
    }

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val windowManager: InputWindowManager by manager.must()
    private val theme by manager.theme()

    private val snackbarCtx by lazy {
        context.withTheme(R.style.InputViewSnackbarTheme)
    }
    private var snackbarInstance: Snackbar? = null

    private lateinit var stateMachine: EventStateMachine<ClipboardStateMachine.State, ClipboardStateMachine.TransitionEvent, ClipboardStateMachine.BooleanKey>

    @Keep
    private val clipboardEnabledListener = ManagedPreference.OnChangeListener<Boolean> { _, it ->
        stateMachine.push(
            ClipboardListeningUpdated, ClipboardListeningEnabled to it
        )
    }

    private val prefs = AppPrefs.getInstance().clipboard
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val clipboardEnabledPref = prefs.clipboardListening
    private val clipboardReturnAfterPaste by prefs.clipboardReturnAfterPaste
    private val clipboardMaskSensitive by prefs.clipboardMaskSensitive

    private val clipboardEntryRadius by ThemeManager.prefs.clipboardEntryRadius

    private var currentCategory = initialCategory ?: ClipboardCategory.All
    private var adapterSubmitJob: Job? = null

    private fun resolveInitialCategory(category: ClipboardCategory?): ClipboardCategory {
        category?.let { return it }
        val saved = sharedPreferences.getString(LAST_OPENED_CATEGORY_KEY, ClipboardCategory.All.name).orEmpty()
        return ClipboardCategory.values().firstOrNull { it.name == saved } ?: ClipboardCategory.All
    }

    private val adapter: ClipboardAdapter by lazy {
        object : ClipboardAdapter(
            theme,
            context.dp(clipboardEntryRadius.toFloat()),
            clipboardMaskSensitive
        ) {
            override fun onPin(id: Int) {
                service.lifecycleScope.launch { ClipboardManager.pin(id) }
            }

            override fun onUnpin(id: Int) {
                service.lifecycleScope.launch { ClipboardManager.unpin(id) }
            }

            override fun onEdit(id: Int) {
                AppUtil.launchClipboardEdit(context, id)
            }

            override fun onOpenFile(entry: ClipboardEntry) {
                val uri = runCatching { Uri.parse(entry.text) }.getOrNull() ?: return
                val mimeType = runCatching { context.contentResolver.getType(uri) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: entry.type.takeIf {
                        it.isNotBlank() && it != ClipDescription.MIMETYPE_TEXT_URILIST
                    }
                    ?: "*/*"
                val target = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(target, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching {
                    service.startActivity(chooser)
                }.onFailure {
                    Toast.makeText(
                        context,
                        it.message ?: context.getString(R.string.unknown_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onShare(entry: ClipboardEntry) {
                val target = if (entry.isUriEntry()) {
                    val uri = Uri.parse(entry.text)
                    val mimeType = runCatching { context.contentResolver.getType(uri) }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: entry.type.takeIf {
                            it.isNotBlank() && it != ClipDescription.MIMETYPE_TEXT_URILIST
                        }
                        ?: "*/*"
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        clipData = ClipData.newUri(context.contentResolver, "clipboard", uri)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, entry.text)
                    }
                }
                val chooser = Intent.createChooser(target, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(chooser)
            }

            override fun shouldShowUpload(entry: ClipboardEntry): Boolean {
                return entry.text.isNotBlank() &&
                    currentCategory in setOf(ClipboardCategory.Favorites, ClipboardCategory.Local)
            }

            override fun onUpload(entry: ClipboardEntry) {
                MainService.submitCapturedClipboard(context, entry.text, "manual-upload")
            }

            override fun onSplitText(text: String) {
                windowManager.attachWindow(TokenizedClipboardWindow(text))
            }

            override fun onSearch(query: String) {
                val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching {
                    service.startActivity(webSearchIntent)
                }
            }

            override fun onDial(number: String) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.fromParts("tel", number, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching {
                    service.startActivity(intent)
                }
            }

            override fun onOpenLink(uri: android.net.Uri) {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching {
                    service.startActivity(intent)
                }
            }

            override fun onViewImage(uri: Uri) {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    service.startActivity(intent)
                }
            }

            override fun onDelete(id: Int) {
                service.lifecycleScope.launch {
                    maybeQueueRemoteMediaSuppression(id)
                    maybeQueueMediaSourceDeletionTarget(id)
                    ClipboardManager.delete(id)
                    showUndoSnackbar(id)
                }
            }

            override fun onPaste(entry: ClipboardEntry) {
                service.commitClipboardEntry(entry.text)
                service.lifecycleScope.launch {
                    ClipboardManager.markUsed(entry.id)
                }
                if (clipboardReturnAfterPaste) windowManager.attachWindow(KeyboardWindow)
            }
        }
    }

    private fun entriesPager(category: ClipboardCategory) = Pager(
        PagingConfig(
            pageSize = 16,
            enablePlaceholders = false
        )
    ) {
        when (category) {
            ClipboardCategory.All -> ClipboardManager.allEntries()
            ClipboardCategory.Favorites -> ClipboardManager.favoriteEntries()
            ClipboardCategory.Local -> ClipboardManager.localTextEntries()
            ClipboardCategory.Media -> ClipboardManager.mediaEntries()
            ClipboardCategory.Remote -> ClipboardManager.remoteEntries()
        }
    }

    private fun submitCategory(category: ClipboardCategory) {
        currentCategory = category
        sharedPreferences.edit { putString(LAST_OPENED_CATEGORY_KEY, category.name) }
        ui.setSelectedCategory(category)
        adapterSubmitJob?.cancel()
        adapterSubmitJob = service.lifecycleScope.launch {
            entriesPager(category).flow.collect {
                adapter.submitData(it)
            }
        }
    }

    private val ui by lazy {
        ClipboardUi(context, theme).apply {
            recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                itemAnimator = null
                adapter = this@ClipboardWindow.adapter
            }
            setSelectedCategory(currentCategory)
            setOnCategorySelectedListener(::submitCategory)
            ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val entry = adapter.getEntryAt(viewHolder.bindingAdapterPosition) ?: return
                    service.lifecycleScope.launch {
                        maybeQueueRemoteMediaSuppression(entry.id)
                        maybeQueueMediaSourceDeletionTarget(entry.id)
                        ClipboardManager.delete(entry.id)
                        showUndoSnackbar(entry.id)
                    }
                }
            }).attachToRecyclerView(recyclerView)
            enableUi.enableButton.setOnClickListener {
                clipboardEnabledPref.setValue(true)
            }
            deleteAllButton.setOnClickListener {
                service.lifecycleScope.launch {
                    promptDeleteAll(ClipboardManager.haveUnpinned(currentCategory))
                }
            }
        }
    }

    override fun onCreateView(): View {
        if (initialCategory == null) {
            currentCategory = resolveInitialCategory(null)
        }
        return ui.root
    }

    private var promptMenu: PopupMenu? = null

    private fun promptDeleteAll(skipPinned: Boolean) {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(context, ui.deleteAllButton).apply {
            menu.add(buildSpannedString {
                bold {
                    color(context.styledColorOrDefault(android.R.attr.colorAccent, theme.genericActiveForegroundColor)) {
                        append(context.getString(if (skipPinned) R.string.delete_all_except_pinned else R.string.delete_all_pinned_items))
                    }
                }
            }).isEnabled = false
            menu.add(android.R.string.cancel)
            menu.item(android.R.string.ok) {
                service.lifecycleScope.launch {
                    deleteEntries(skipPinned, deleteFiles = false)
                }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }

    private val pendingDeleteIds = arrayListOf<Int>()
    private val pendingSuppressedRemoteContents = linkedSetOf<String>()
    private val pendingDeleteSourceTargets = linkedMapOf<Int, ClipboardSourceDeletionTarget>()

    private suspend fun deleteEntries(skipPinned: Boolean, deleteFiles: Boolean) {
        if (currentCategory == ClipboardCategory.Media || currentCategory == ClipboardCategory.Remote) {
            pendingSuppressedRemoteContents += ClipboardManager.remoteMediaSuppressionContents(skipPinned)
        }
        if (currentCategory == ClipboardCategory.Media) {
            pendingDeleteSourceTargets.putAll(ClipboardManager.mediaDeletionTargets(skipPinned))
        } else if (currentCategory == ClipboardCategory.Remote) {
            pendingDeleteSourceTargets.putAll(
                ClipboardManager.mediaDeletionTargetsBySource(
                    source = ClipboardEntry.SOURCE_REMOTE,
                    skipPinned = skipPinned
                )
            )
        }
        val ids = ClipboardManager.deleteAll(currentCategory, skipPinned)
        showUndoSnackbar(*ids)
    }

    private suspend fun maybeQueueRemoteMediaSuppression(id: Int) {
        ClipboardManager.remoteMediaSuppressionContent(id)?.let { pendingSuppressedRemoteContents += it }
    }

    private suspend fun maybeQueueMediaSourceDeletionTarget(id: Int) {
        ClipboardManager.mediaDeletionTarget(id)?.let { pendingDeleteSourceTargets[id] = it }
    }

    private fun notifyClipboardPluginSuppressedRemoteContents(contents: Collection<String>) {
        if (contents.isEmpty()) return
        MainService.suppressRemoteClipboardContents(context, contents)
    }

    @SuppressLint("RestrictedApi")
    private fun showUndoSnackbar(vararg id: Int) {
        id.forEach { pendingDeleteIds.add(it) }
        val str = context.resources.getString(R.string.num_items_deleted, pendingDeleteIds.size)
        snackbarInstance = Snackbar.make(snackbarCtx, ui.root, str, Snackbar.LENGTH_LONG)
            .setBackgroundTint(theme.popupBackgroundColor)
            .setTextColor(theme.popupTextColor)
            .setActionTextColor(theme.genericActiveBackgroundColor)
            .setAction(R.string.undo) {
                service.lifecycleScope.launch {
                    ClipboardManager.undoDelete(*pendingDeleteIds.toIntArray())
                    pendingDeleteIds.clear()
                    pendingSuppressedRemoteContents.clear()
                    pendingDeleteSourceTargets.clear()
                }
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    if (snackbarInstance === transientBottomBar) {
                        snackbarInstance = null
                    }
                    when (event) {
                        BaseCallback.DISMISS_EVENT_SWIPE,
                        BaseCallback.DISMISS_EVENT_MANUAL,
                        BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                            service.lifecycleScope.launch {
                                notifyClipboardPluginSuppressedRemoteContents(pendingSuppressedRemoteContents)
                                ClipboardManager.deleteClipboardSourceFiles(pendingDeleteSourceTargets.values)
                                ClipboardManager.realDelete()
                                pendingDeleteIds.clear()
                                pendingSuppressedRemoteContents.clear()
                                pendingDeleteSourceTargets.clear()
                            }
                        }

                        BaseCallback.DISMISS_EVENT_ACTION,
                        BaseCallback.DISMISS_EVENT_CONSECUTIVE -> {
                            // user clicked "undo" or deleted more items which makes a new snackbar
                        }
                    }
                }
            }).apply {
                val hMargin = snackbarCtx.dp(24)
                val vMargin = snackbarCtx.dp(16)
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    leftMargin = hMargin
                    rightMargin = hMargin
                    bottomMargin = vMargin
                }
                ((view as FrameLayout).getChildAt(0) as SnackbarContentLayout).apply {
                    messageView.letterSpacing = 0f
                    actionView.letterSpacing = 0f
                }
                show()
            }
    }

    override fun onAttached() {
        val isEmpty = ClipboardManager.itemCount == 0
        val isListening = clipboardEnabledPref.getValue()
        val initialState = when {
            !isListening -> EnableListening
            isEmpty -> AddMore
            else -> Normal
        }
        stateMachine = ClipboardStateMachine.new(initialState, isEmpty, isListening) {
            ui.switchUiByState(it)
        }
        // manually switch to initial ui
        ui.switchUiByState(initialState)
        adapter.addLoadStateListener {
            val empty = it.append.endOfPaginationReached && adapter.itemCount < 1
            stateMachine.push(ClipboardDbUpdated, ClipboardDbEmpty to empty)
        }
        submitCategory(currentCategory)
        clipboardEnabledPref.registerOnChangeListener(clipboardEnabledListener)
    }

    override fun onDetached() {
        clipboardEnabledPref.unregisterOnChangeListener(clipboardEnabledListener)
        adapter.onDetached()
        adapterSubmitJob?.cancel()
        promptMenu?.dismiss()
        snackbarInstance?.dismiss()
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override fun onCreateBarExtension(): View = ui.extension
}
