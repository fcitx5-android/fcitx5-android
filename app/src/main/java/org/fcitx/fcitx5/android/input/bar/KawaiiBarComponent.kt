package org.fcitx.fcitx5.android.input.bar

import android.graphics.Color
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ViewAnimator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateStyle
import org.fcitx.fcitx5.android.input.candidates.expanded.window.FlexboxExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.candidates.expanded.window.GridExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.editing.TextEditingWindow
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.status.StatusAreaWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.AppUtil
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import splitties.bitflags.hasFlag
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val service by manager.inputMethodService()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()

    private val clipboardSuggestion = AppPrefs.getInstance().clipboard.clipboardSuggestion
    private val clipboardItemTimeout = AppPrefs.getInstance().clipboard.clipboardItemTimeout
    private val expandedCandidateStyle by AppPrefs.getInstance().keyboard.expandedCandidateStyle
    private val expandToolbarByDefault = AppPrefs.getInstance().keyboard.expandToolbarByDefault

    private var clipboardTimeoutJob: Job? = null

    private val onClipboardUpdateListener =
        ClipboardManager.OnClipboardUpdateListener {
            if (!clipboardSuggestion.getValue()) return@OnClipboardUpdateListener
            service.lifecycleScope.launch {
                if (it.text.isEmpty()) {
                    idleUiStateMachine.push(ClipboardUpdatedEmpty)
                } else {
                    idleUi.setClipboardItemText(it.text.take(42))
                    idleUiStateMachine.push(ClipboardUpdatedNonEmpty)
                    launchClipboardTimeoutJob()
                }
            }
        }

    private val onClipboardSuggestionUpdateListener =
        ManagedPreference.OnChangeListener<Boolean> { _, it ->
            if (!it) {
                idleUiStateMachine.push(ClipboardUpdatedEmpty)
                clipboardTimeoutJob?.cancel()
                clipboardTimeoutJob = null
            }
        }

    private val onClipboardTimeoutUpdateListener =
        ManagedPreference.OnChangeListener<Int> { _, _ ->
            when (idleUiStateMachine.currentState) {
                IdleUiStateMachine.State.Clipboard,
                IdleUiStateMachine.State.ToolbarWithClip -> {
                    // renew timeout when clipboard suggestion is present
                    launchClipboardTimeoutJob()
                }

                else -> {}
            }
        }

    private val onExpandToolbarByDefaultUpdateListener =
        ManagedPreference.OnChangeListener<Boolean> { _, it ->
            idleUiStateMachine = IdleUiStateMachine.new(it, idleUiStateMachine)
        }

    private val popupActionListener by lazy {
        popup.listener
    }

    init {
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
        clipboardSuggestion.registerOnChangeListener(onClipboardSuggestionUpdateListener)
        clipboardItemTimeout.registerOnChangeListener(onClipboardTimeoutUpdateListener)
        expandToolbarByDefault.registerOnChangeListener(onExpandToolbarByDefaultUpdateListener)
    }

    private fun launchClipboardTimeoutJob() {
        clipboardTimeoutJob?.cancel()
        val timeout = clipboardItemTimeout.getValue() * 1000L
        // never transition to ClipboardTimedOut state when timeout < 0
        if (timeout < 0L) return
        clipboardTimeoutJob = service.lifecycleScope.launch {
            delay(timeout)
            idleUiStateMachine.push(Timeout)
            clipboardTimeoutJob = null
        }
    }

    private val idleUi: KawaiiBarUi.Idle by lazy {
        KawaiiBarUi.Idle(context, theme) { idleUiStateMachine.currentState }.apply {
            menuButton.setOnClickListener {
                idleUiStateMachine.push(MenuButtonClicked)
                // reset timeout timer (if present) when user switch layout
                if (clipboardTimeoutJob != null) {
                    launchClipboardTimeoutJob()
                }
            }
            undoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true)
            }
            redoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true, shift = true)
            }
            cursorMoveButton.setOnClickListener {
                windowManager.attachWindow(TextEditingWindow())
            }
            clipboardButton.setOnClickListener {
                windowManager.attachWindow(ClipboardWindow())
            }
            moreButton.setOnClickListener {
                windowManager.attachWindow(StatusAreaWindow())
            }
            clipboardSuggestionItem.setOnClickListener {
                ClipboardManager.lastEntry?.let {
                    service.commitText(it.text)
                }
                clipboardTimeoutJob?.cancel()
                clipboardTimeoutJob = null
                idleUiStateMachine.push(Pasted)
            }
            clipboardSuggestionItem.setOnLongClickListener {
                ClipboardManager.lastEntry?.let {
                    AppUtil.launchClipboardEdit(context, it.id, true)
                }
                true
            }
            hideKeyboardButton.setOnClickListener {
                service.requestHideSelf(0)
            }
            switchUiByState(idleUiStateMachine.currentState)
        }
    }

    private val candidateUi by lazy {
        KawaiiBarUi.Candidate(context, theme, horizontalCandidate.view)
    }

    private val titleUi by lazy { KawaiiBarUi.Title(context, theme) }

    private val numberRowUi by lazy { KawaiiBarUi.NumberRowUi(context, theme) }

    private val barStateMachine = KawaiiBarStateMachine.new {
        switchUiByState(it)
    }

    val expandButtonStateMachine = ExpandButtonStateMachine.new {
        when (it) {
            ClickToAttachWindow -> {
                setExpandButtonToAttach()
                setExpandButtonEnabled(true)
            }

            ClickToDetachWindow -> {
                setExpandButtonToDetach()
                setExpandButtonEnabled(true)
            }

            Hidden -> {
                setExpandButtonEnabled(false)
            }
        }
    }

    private var idleUiStateMachine = IdleUiStateMachine.new(expandToolbarByDefault.getValue()) {
        idleUi.switchUiByState(it)
    }

    // set expand candidate button to create expand candidate
    private fun setExpandButtonToAttach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.attachWindow(
                when (expandedCandidateStyle) {
                    ExpandedCandidateStyle.Grid -> GridExpandedCandidateWindow()
                    ExpandedCandidateStyle.Flexbox -> FlexboxExpandedCandidateWindow()
                }
            )
        }
        candidateUi.expandButton.image.imageResource = R.drawable.ic_baseline_expand_more_24
    }

    // set expand candidate button to close expand candidate
    private fun setExpandButtonToDetach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.attachWindow(KeyboardWindow)
        }
        candidateUi.expandButton.image.imageResource = R.drawable.ic_baseline_expand_less_24
    }

    // should be used with setExpandButtonToAttach or setExpandButtonToDetach
    private fun setExpandButtonEnabled(enabled: Boolean) {
        if (enabled)
            candidateUi.expandButton.visibility = View.VISIBLE
        else
            candidateUi.expandButton.visibility = View.INVISIBLE
    }

    private fun switchUiByState(state: KawaiiBarStateMachine.State) {
        val index = state.ordinal
        if (view.displayedChild == index)
            return
        Timber.d("Switch bar to $state")
        val new = view.getChildAt(index)
        if (new != titleUi.root) {
            titleUi.setReturnButtonOnClickListener { }
            titleUi.setTitle("")
            titleUi.removeExtension()
        }
        if (new == numberRowUi.root) {
            numberRowUi.root.keyActionListener = commonKeyActionListener.listener
            numberRowUi.root.popupActionListener = popupActionListener
        } else {
            numberRowUi.root.keyActionListener = null
            numberRowUi.root.popupActionListener = null
            popup.dismissAll()
        }
        view.displayedChild = index
    }

    override val view by lazy {
        ViewAnimator(context).apply {
            backgroundColor =
                if (ThemeManager.prefs.keyBorder.getValue()) Color.TRANSPARENT
                else theme.barColor
            add(idleUi.root, lParams(matchParent, matchParent))
            add(candidateUi.root, lParams(matchParent, matchParent))
            add(titleUi.root, lParams(matchParent, matchParent))
            add(numberRowUi.root, lParams(matchParent, matchParent))
        }
    }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        ClipboardManager.lastEntry?.let {
            val now = System.currentTimeMillis()
            val clipboardTimeout = clipboardItemTimeout.getValue() * 1000L
            if (now - it.timestamp < clipboardTimeout) {
                onClipboardUpdateListener.onUpdate(it)
            }
        }
    }

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            idleUi.privateMode(info.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        }
        idleUiStateMachine.push(KawaiiBarShown)
        barStateMachine.push(
            if (capFlags.has(CapabilityFlag.Password))
                CapFlagsUpdatedPassword
            else
                CapFlagsUpdatedNoPassword
        )
    }

    override fun onPreeditUpdate(data: FcitxEvent.PreeditEvent.Data) {
        barStateMachine.push(
            if (data.preedit.isEmpty() && data.clientPreedit.isEmpty())
                PreeditUpdatedEmpty
            else
                PreeditUpdatedNonEmpty
        )
    }

    override fun onCandidateUpdate(data: Array<String>) {
        if (data.isNotEmpty())
            barStateMachine.push(CandidateUpdateNonEmpty)
    }

    override fun onWindowAttached(window: InputWindow) {
        when (window) {
            is InputWindow.ExtendedInputWindow<*> -> {
                titleUi.setTitle(window.title)
                window.onCreateBarExtension()?.let { titleUi.addExtension(it, window.showTitle) }
                titleUi.setReturnButtonOnClickListener {
                    windowManager.attachWindow(KeyboardWindow)
                }
                barStateMachine.push(ExtendedWindowAttached)
            }

            is InputWindow.SimpleInputWindow<*> -> {
            }
        }
    }

    override fun onWindowDetached(window: InputWindow) {
        barStateMachine.push(
            if (horizontalCandidate.adapter.candidates.isEmpty())
                if (service.capabilityFlags.has(CapabilityFlag.Password))
                    WindowDetachedWithCapFlagsPassword
                else
                    WindowDetachedWithCapFlagsNoPassword
            else
                WindowDetachedWithCandidatesNonEmpty
        )
    }

}
