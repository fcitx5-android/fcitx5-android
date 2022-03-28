package org.fcitx.fcitx5.android.input.clipboard

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.*
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.inputConnection
import kotlin.properties.Delegates

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()

    private lateinit var stateMachine: EventStateMachine<ClipboardStateMachine.State, ClipboardStateMachine.TransitionEvent>

    private var isClipboardDbEmpty by Delegates.observable(true) { _, _, new ->
        stateMachine.push(
            if (new) ClipboardDbUpdatedEmpty
            else ClipboardDbUpdatedNonEmpty
        )
    }

    private val clipboardListeningListener: Prefs.OnChangeListener<Boolean> by lazy {
        Prefs.OnChangeListener {
            pushClipboardListening(value)
        }
    }

    private fun pushClipboardListening(enabled: Boolean) {
        if (enabled)
            stateMachine.push(
                if (isClipboardDbEmpty)
                    ClipboardListeningEnabledWithDbEmpty
                else
                    ClipboardListeningEnabledWithDbNonEmpty
            )
        else
            stateMachine.push(ClipboardListeningDisabled)
    }

    private val clipboardListeningPref by lazy {
        Prefs.getInstance().clipboardListening
    }

    private val onClipboardUpdateListener by lazy {
        ClipboardManager.OnClipboardUpdateListener {
            service.lifecycleScope.launch {
                adapter.updateEntries(ClipboardManager.getAll().also {
                    isClipboardDbEmpty = it.isEmpty()
                })
            }
        }
    }

    private val adapter: ClipboardAdapter by lazy {
        object : ClipboardAdapter() {
            override suspend fun onPin(id: Int) = ClipboardManager.pin(id)
            override suspend fun onUnpin(id: Int) = ClipboardManager.unpin(id)
            override suspend fun onDelete(id: Int) = ClipboardManager.delete(id)
            override fun onPaste(id: Int) {
                service.inputConnection?.commitText(getEntryById(id).text, 1)
            }
        }
    }

    private val ui by lazy {
        ClipboardUi(context).apply {
            recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = this@ClipboardWindow.adapter
            }
            enableUi.enableButton.setOnClickListener {
                clipboardListeningPref.value = true
            }
            deleteAllButton.setOnClickListener {
                service.lifecycleScope.launch {
                    ClipboardManager.deleteAll()
                    adapter.updateEntries(emptyList())
                    isClipboardDbEmpty = true
                }
            }
        }
    }

    override val view by lazy {
        ui.root
    }

    override fun onAttached() {
        service.lifecycleScope.launch {
            val entries = ClipboardManager.getAll()
            val initialState = when {
                !clipboardListeningPref.value -> EnableListening
                entries.isEmpty() -> AddMore
                else -> Normal
            }
            stateMachine = ClipboardStateMachine.new(initialState) {
                ui.switchUiByState(it)
            }
            adapter.updateEntries(entries)
            clipboardListeningPref.registerOnChangeListener(clipboardListeningListener)
            pushClipboardListening(clipboardListeningPref.value)
            ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
            // manually switch to initial ui
            ui.switchUiByState(initialState)
        }
    }

    override fun onDetached() {
        ClipboardManager.removeOnUpdateListener(onClipboardUpdateListener)
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override val barExtension: View by lazy {
        ui.extension
    }
}