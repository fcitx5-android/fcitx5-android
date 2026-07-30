/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.annotation.Keep
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.SnackbarContentLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.clipboard.ClipboardEntryFilter
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.item
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.core.withTheme

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val windowManager: InputWindowManager by manager.must()
    private val theme by manager.theme()

    private val snackbarCtx by lazy {
        context.withTheme(R.style.InputViewSnackbarTheme)
    }
    private var snackbarInstance: Snackbar? = null

    @Keep
    private val clipboardEnabledListener = ManagedPreference.OnChangeListener<Boolean> { _, _ ->
        renderUi()
    }

    private val prefs = AppPrefs.getInstance().clipboard

    private val clipboardEnabledPref = prefs.clipboardListening
    private val clipboardReturnAfterPaste by prefs.clipboardReturnAfterPaste
    private val clipboardMaskSensitive by prefs.clipboardMaskSensitive

    private val clipboardEntryRadius by ThemeManager.prefs.clipboardEntryRadius

    private var adapterSubmitJob: Job? = null

    private var selectedSection = ClipboardPanelSection.Clipboard
    private var selectedCategory: ClipboardCategory? = null
    private var visibleEntriesEmpty = true
    private var deleteAvailable = false

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

            override fun onFavorite(id: Int) {
                service.lifecycleScope.launch { ClipboardManager.favorite(id) }
            }

            override fun onUnfavorite(id: Int) {
                service.lifecycleScope.launch { ClipboardManager.unfavorite(id) }
            }

            override fun onEdit(id: Int) {
                AppUtil.launchClipboardEdit(context, id)
            }

            override fun onShare(entry: ClipboardEntry) {
                val target = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, entry.text)
                }
                val chooser = Intent.createChooser(target, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(chooser)
            }

            override fun onDelete(id: Int) {
                service.lifecycleScope.launch {
                    ClipboardManager.delete(id)
                    showUndoSnackbar(id)
                }
            }

            override fun onPaste(entry: ClipboardEntry) {
                service.commitText(entry.text)
                if (clipboardReturnAfterPaste) windowManager.attachWindow(KeyboardWindow)
            }
        }
    }

    private val ui by lazy {
        ClipboardUi(context, theme).apply {
            recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = this@ClipboardWindow.adapter
            }
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
                        ClipboardManager.delete(entry.id)
                        showUndoSnackbar(entry.id)
                    }
                }
            }).attachToRecyclerView(recyclerView)
            enableUi.enableButton.setOnClickListener {
                clipboardEnabledPref.setValue(true)
            }
            topBar.setOnSectionSelectedListener {
                showSection(it)
            }
            categoryBar.setOnCategorySelectedListener {
                showCategory(it)
            }
            topBar.deleteAllButton.setOnClickListener {
                promptDeleteAll()
            }
        }
    }

    override fun onCreateView(): View = ui.root

    private var promptMenu: PopupMenu? = null

    private fun promptDeleteAll() {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(context, ui.topBar.deleteAllButton).apply {
            menu.add(buildSpannedString {
                bold {
                    color(context.styledColor(android.R.attr.colorAccent)) {
                        append(context.getString(R.string.delete_all_except_pinned_and_favorites))
                    }
                }
            }).isEnabled = false
            menu.add(android.R.string.cancel)
            menu.item(android.R.string.ok) {
                service.lifecycleScope.launch {
                    val ids = ClipboardManager.deleteAll()
                    showUndoSnackbar(*ids)
                }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }

    private val pendingDeleteIds = arrayListOf<Int>()

    @SuppressLint("RestrictedApi")
    private fun showUndoSnackbar(vararg id: Int) {
        if (id.isEmpty()) return
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
                                ClipboardManager.realDelete()
                                pendingDeleteIds.clear()
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

    private val adapterLoadStateListener: (CombinedLoadStates) -> Unit = {
        if (it.refresh is LoadState.NotLoading) {
            visibleEntriesEmpty = it.append.endOfPaginationReached && adapter.itemCount < 1
            refreshDeleteAvailability()
            renderUi()
        }
    }

    private fun showSection(section: ClipboardPanelSection) {
        selectedSection = section
        ui.topBar.setActiveSection(section)
        submitEntries()
    }

    private fun showCategory(category: ClipboardCategory?) {
        selectedCategory = category
        ui.categoryBar.setActiveCategory(category)
        submitEntries()
    }

    private fun submitEntries() {
        visibleEntriesEmpty = false
        deleteAvailable = false
        renderUi()
        adapterSubmitJob?.cancel()
        adapterSubmitJob = service.lifecycleScope.launch {
            val filter = ClipboardEntryFilter(
                scope = when (selectedSection) {
                    ClipboardPanelSection.Clipboard -> ClipboardEntryFilter.Scope.All
                    ClipboardPanelSection.Favorites -> ClipboardEntryFilter.Scope.Favorites
                },
                category = selectedCategory
            )
            Pager(PagingConfig(pageSize = 16)) { ClipboardManager.entries(filter) }
                .flow
                .collectLatest { adapter.submitData(it) }
        }
    }

    private fun refreshDeleteAvailability() {
        val section = selectedSection
        service.lifecycleScope.launch {
            val available = ClipboardManager.haveDeletable()
            if (selectedSection == section) {
                deleteAvailable = available
                renderUi()
            }
        }
    }

    private fun renderUi() {
        selectedCategory?.let { ui.filteredEmptyUi.setFilter(selectedSection, it) }
        val state = ClipboardStateMachine.resolve(
            clipboardEnabledPref.getValue(),
            selectedSection,
            selectedCategory != null,
            visibleEntriesEmpty
        )
        ui.switchUiByState(
            state,
            state == ClipboardStateMachine.State.Normal &&
                selectedSection == ClipboardPanelSection.Clipboard &&
                selectedCategory == null &&
                deleteAvailable
        )
    }

    override fun onAttached() {
        selectedSection = ClipboardPanelSection.Clipboard
        selectedCategory = null
        visibleEntriesEmpty = ClipboardManager.itemCount == 0
        deleteAvailable = false
        ui.topBar.setActiveSection(selectedSection)
        ui.categoryBar.setActiveCategory(selectedCategory)
        adapter.addLoadStateListener(adapterLoadStateListener)
        renderUi()
        showSection(selectedSection)
        clipboardEnabledPref.registerOnChangeListener(clipboardEnabledListener)
    }

    override fun onDetached() {
        clipboardEnabledPref.unregisterOnChangeListener(clipboardEnabledListener)
        adapter.removeLoadStateListener(adapterLoadStateListener)
        adapter.onDetached()
        adapterSubmitJob?.cancel()
        adapterSubmitJob = null
        promptMenu?.dismiss()
        snackbarInstance?.dismiss()
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override val showTitle = false

    override val showReturnButton = true

    override fun onCreateBarExtension(): View = ui.extension
}
