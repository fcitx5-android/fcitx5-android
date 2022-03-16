package org.fcitx.fcitx5.android.input.clipboard

import android.view.View
import android.widget.ImageView
import android.widget.ViewAnimator
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
import org.fcitx.fcitx5.android.utils.times
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource
import timber.log.Timber
import kotlin.properties.Delegates

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()

    private val layoutManager by lazy {
        StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL
        )
    }

    private var isClipboardDbEmpty by Delegates.observable(true) { _, _, new ->
        stateMachine.push(
            if (new)
                ClipboardDbUpdatedEmpty
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
                ClipboardListeningEnabled * (
                        if (isClipboardDbEmpty)
                            ClipboardDbUpdatedEmpty
                        else
                            ClipboardDbUpdatedNonEmpty)
            )
        else
            stateMachine.push(ClipboardListeningDisabled)
    }

    private val enableClipboardListeningPref by lazy { Prefs.getInstance().clipboardListening }

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

            override fun onPaste(id: Int) {
                service.inputConnection?.commitText(getEntryById(id).text, 1)
            }

            override suspend fun onPin(id: Int) {
                ClipboardManager.pin(id)
            }

            override suspend fun onUnpin(id: Int) {
                ClipboardManager.unpin(id)
            }

            override suspend fun onDelete(id: Int) {
                ClipboardManager.delete(id)
            }
        }
    }

    private val deleteAllButton by lazy {
        context.imageButton {
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            imageResource = R.drawable.ic_baseline_delete_sweep_24
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            visibility = View.INVISIBLE
            setOnClickListener {
                service.lifecycleScope.launch {
                    ClipboardManager.deleteAll()
                    adapter.updateEntries(emptyList())
                    isClipboardDbEmpty = true
                }
            }
        }
    }

    private val recyclerView by lazy {
        context.recyclerView {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            layoutManager = this@ClipboardWindow.layoutManager
            adapter = this@ClipboardWindow.adapter
            addItemDecoration(SpacesItemDecoration(dp(4)))
        }
    }

    private val instructionText by lazy {
        context.textView {
            textSize = 30f
        }
    }

    private lateinit var stateMachine: EventStateMachine<ClipboardStateMachine.State, ClipboardStateMachine.TransitionEvent>

    private fun setDeleteButtonEnabled(enabled: Boolean) {
        deleteAllButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun switchUiByState(state: ClipboardStateMachine.State) {
        Timber.d("Switch clipboard to $state")
        when (state) {
            Normal -> {
                view.displayedChild = 0
                instructionText.text = ""
                setDeleteButtonEnabled(true)
            }
            AddMore -> {
                view.displayedChild = 1
                instructionText.setText(R.string.instruction_copy)
                setDeleteButtonEnabled(false)
            }
            EnableListening -> {
                view.displayedChild = 1
                instructionText.setText(R.string.instruction_enable_clipboard_listening)
                setDeleteButtonEnabled(false)
            }
        }
    }

    override val view by lazy {
        ViewAnimator(context).apply {
            add(recyclerView, lParams(matchParent, matchParent))
            add(instructionText, lParams(matchParent, wrapContent))
        }
    }

    override fun onAttached() {
        service.lifecycleScope.launch {
            val entries = ClipboardManager.getAll()
            val initialState = when {
                !enableClipboardListeningPref.value -> EnableListening
                entries.isEmpty() -> AddMore
                else -> Normal
            }
            stateMachine = ClipboardStateMachine.new(initialState) {
                switchUiByState(it)
            }
            adapter.updateEntries(entries)
            enableClipboardListeningPref.registerOnChangeListener(clipboardListeningListener)
            pushClipboardListening(enableClipboardListeningPref.value)
            ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
            // manually switch to initial ui
            switchUiByState(initialState)
        }
    }

    override fun onDetached() {
        ClipboardManager.removeOnUpdateListener(onClipboardUpdateListener)
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override val barExtension: View by lazy {
        context.horizontalLayout {
            add(deleteAllButton, lParams(dp(40), dp(40)))
        }
    }
}