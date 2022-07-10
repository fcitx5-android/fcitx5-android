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
import org.fcitx.fcitx5.android.input.status.StatusAreaWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.inputConnection
import org.mechdancer.dependency.manager.must
import splitties.bitflags.hasFlag
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val service by manager.inputMethodService()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()

    private val clipboardItemTimeout by AppPrefs.getInstance().clipboard.clipboardItemTimeout
    private val expandedCandidateStyle by AppPrefs.getInstance().keyboard.expandedCandidateStyle
    private val expandToolbarByDefault = AppPrefs.getInstance().keyboard.expandToolbarByDefault

    private var clipboardTimeoutJob: Job? = null

    private val onClipboardUpdateListener =
        ClipboardManager.OnClipboardUpdateListener {
            service.lifecycleScope.launch {
                if (it.isEmpty()) {
                    idleUiStateMachine.push(ClipboardUpdatedEmpty)
                } else {
                    idleUi.setClipboardItemText(it.take(42))
                    idleUiStateMachine.push(ClipboardUpdatedNonEmpty)
                    launchClipboardTimeoutJob()
                }
            }
        }

    private val onExpandToolbarByDefaultUpdateListener =
        ManagedPreference.OnChangeListener<Boolean> {
            idleUiStateMachine = IdleUiStateMachine.new(it, idleUiStateMachine)
        }

    init {
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
        expandToolbarByDefault.registerOnChangeListener(onExpandToolbarByDefaultUpdateListener)
    }

    private fun launchClipboardTimeoutJob() {
        clipboardTimeoutJob?.cancel()
        clipboardTimeoutJob = service.lifecycleScope.launch {
            delay(clipboardItemTimeout.seconds)
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
                service.inputConnection?.performContextMenuAction(android.R.id.paste)
                clipboardTimeoutJob?.cancel()
                clipboardTimeoutJob = null
                idleUiStateMachine.push(Pasted)
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
            windowManager.switchToKeyboardWindow()
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

    fun onShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            idleUi.privateMode(
                service.editorInfo?.imeOptions?.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == true
            )
        }
        idleUiStateMachine.push(KawaiiBarShown)
    }

    private fun switchUiByState(state: KawaiiBarStateMachine.State) {
        val index = state.ordinal
        if (view.displayedChild == index)
            return
        Timber.d("Switch bar to $state")
        if (view.getChildAt(index) != titleUi.root) {
            titleUi.setReturnButtonOnClickListener { }
            titleUi.setTitle("")
            titleUi.removeExtension()
        }
        view.displayedChild = index
    }

    override val view by lazy {
        ViewAnimator(context).apply {
            backgroundColor =
                if (ThemeManager.prefs.keyBorder.getValue()) Color.TRANSPARENT
                else theme.barColor.color
            add(idleUi.root, lParams(matchParent, matchParent))
            add(candidateUi.root, lParams(matchParent, matchParent))
            add(titleUi.root, lParams(matchParent, matchParent))
        }
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
                window.onCreateBarExtension()?.let { titleUi.addExtension(it) }
                titleUi.setReturnButtonOnClickListener {
                    windowManager.switchToKeyboardWindow()
                }
                barStateMachine.push(ExtendedWindowAttached)
            }
            is InputWindow.SimpleInputWindow<*> -> {
            }
        }
    }

    override fun onWindowDetached(window: InputWindow) {
        barStateMachine.push(WindowDetached)
    }
}