package org.fcitx.fcitx5.android.input.clipboard

import android.view.View
import android.widget.PopupMenu
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.Theme
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
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.EventStateMachine
import splitties.resources.styledColor
import kotlin.properties.Delegates

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val theme by manager.theme()

    private lateinit var stateMachine: EventStateMachine<ClipboardStateMachine.State, ClipboardStateMachine.TransitionEvent, ClipboardStateMachine.BooleanKey>

    private var isClipboardDbEmpty by Delegates.observable(ClipboardManager.itemCount == 0) { _, _, new ->
        stateMachine.push(
            ClipboardDbUpdated, ClipboardDbEmpty to new
        )
    }

    private val clipboardEnabledListener = ManagedPreference.OnChangeListener<Boolean> { _, it ->
        stateMachine.push(
            ClipboardListeningUpdated, ClipboardListeningEnabled to it
        )
    }

    private val clipboardEnabledPref = AppPrefs.getInstance().clipboard.clipboardListening

    private fun updateClipboardEntries() {
        service.lifecycleScope.launch {
            ClipboardManager.getAll().also {
                isClipboardDbEmpty = it.isEmpty()
                adapter.updateEntries(it)
            }
        }
    }

    private fun deleteAllEntries(skipPinned: Boolean = true) {
        service.lifecycleScope.launch {
            ClipboardManager.deleteAll(skipPinned)
            if (skipPinned) {
                ClipboardManager.getAll().also {
                    isClipboardDbEmpty = it.isEmpty()
                    adapter.updateEntries(it)
                }
            } else {
                // manually set entries to empty
                adapter.updateEntries(emptyList())
                isClipboardDbEmpty = true
            }
        }
    }

    private val onClipboardUpdateListener = ClipboardManager.OnClipboardUpdateListener {
        updateClipboardEntries()
    }

    private val adapter: ClipboardAdapter by lazy {
        object : ClipboardAdapter() {
            override suspend fun onPin(id: Int) = ClipboardManager.pin(id)
            override suspend fun onUnpin(id: Int) = ClipboardManager.unpin(id)
            override fun onEdit(id: Int) = AppUtil.launchClipboardEdit(context, id)
            override suspend fun onDelete(id: Int) {
                ClipboardManager.delete(id)
                isClipboardDbEmpty = entries.isEmpty()
            }

            override fun onPaste(id: Int) {
                service.commitText(getEntryById(id).text)
            }

            override val theme: Theme
                get() = this@ClipboardWindow.theme
        }
    }

    private val ui by lazy {
        ClipboardUi(context, theme).apply {
            recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = this@ClipboardWindow.adapter
            }
            enableUi.enableButton.setOnClickListener {
                clipboardEnabledPref.setValue(true)
            }
            deleteAllButton.setOnClickListener {
                if (adapter.entries.any { !it.pinned }) {
                    deleteAllEntries()
                } else {
                    promptDeleteAllPinned()
                }
            }
        }
    }

    override fun onCreateView(): View = ui.root

    private var promptMenu: PopupMenu? = null

    private fun promptDeleteAllPinned() {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(context, ui.deleteAllButton).apply {
            menu.apply {
                add(buildSpannedString {
                    bold {
                        color(context.styledColor(android.R.attr.colorAccent)) {
                            append(context.getString(R.string.delete_all_pinned_items))
                        }
                    }
                }).apply {
                    isEnabled = false
                }
                add(android.R.string.cancel).apply {
                    setOnMenuItemClickListener { true }
                }
                add(android.R.string.ok).apply {
                    setOnMenuItemClickListener {
                        deleteAllEntries(false)
                        true
                    }
                }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }

    override fun onAttached() {
        val initialState = when {
            !clipboardEnabledPref.getValue() -> EnableListening
            isClipboardDbEmpty -> AddMore
            else -> Normal
        }
        stateMachine = ClipboardStateMachine.new(initialState) {
            ui.switchUiByState(it)
        }
        // manually switch to initial ui
        ui.switchUiByState(initialState)
        // manually sync clipboard entries form db
        updateClipboardEntries()
        clipboardEnabledPref.registerOnChangeListener(clipboardEnabledListener)
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
    }

    override fun onDetached() {
        clipboardEnabledPref.unregisterOnChangeListener(clipboardEnabledListener)
        ClipboardManager.removeOnUpdateListener(onClipboardUpdateListener)
        adapter.onDetached()
        promptMenu?.dismiss()
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override fun onCreateBarExtension(): View = ui.extension
}