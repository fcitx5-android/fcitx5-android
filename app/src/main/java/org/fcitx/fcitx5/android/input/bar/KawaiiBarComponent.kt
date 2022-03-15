package org.fcitx.fcitx5.android.input.bar

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ViewAnimator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.input.bar.ExpandButtonState.*
import org.fcitx.fcitx5.android.input.bar.ExpandButtonTransitionEvent.*
import org.fcitx.fcitx5.android.input.bar.IdleUiState.*
import org.fcitx.fcitx5.android.input.bar.IdleUiTransitionEvent.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarState.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarTransitionEvent.*
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.ExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.eventStateMachine
import org.fcitx.fcitx5.android.utils.inputConnection
import org.mechdancer.dependency.manager.must
import splitties.bitflags.hasFlag
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val windowManager: InputWindowManager by manager.must()
    private val service by manager.inputMethodService()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()

    private val clipboardItemTimeout by Prefs.getInstance().clipboardItemTimeout

    private val onClipboardUpdateListener by lazy {
        ClipboardManager.OnClipboardUpdateListener {
            service.lifecycleScope.launch {
                idleUi.setClipboardItemText(it)
                idleUiStateMachine.push(ClipboardUpdatedNonEmpty)
                launch {
                    val current = ++timeoutJobId
                    delay(clipboardItemTimeout.seconds)
                    if (timeoutJobId == current)
                        idleUiStateMachine.push(Timeout)
                }
            }
        }
    }

    init {
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
    }

    private val idleUi: KawaiiBarUi.Idle by lazy {
        KawaiiBarUi.Idle(context) { idleUiStateMachine.currentState }.also {
            it.menuButton.setOnClickListener {
                idleUiStateMachine.push(
                    if (idleUi.getClipboardItemText().isEmpty())
                        MenuButtonClickedWithClipboardEmpty
                    else
                        MenuButtonClickedWithClipboardNonEmpty
                )
            }
            it.undoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true)
            }
            it.redoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true, shift = true)
            }
            it.cursorMoveButton.setOnClickListener { }
            it.clipboardButton.setOnClickListener {
                windowManager.attachWindow(ClipboardWindow())
            }
            it.settingsButton.setOnClickListener {
                AppUtil.launchMain(context)
            }
            it.clipboardItem.setOnClickListener {
                service.inputConnection?.performContextMenuAction(android.R.id.paste)
                idleUiStateMachine.push(Pasted)
            }
            it.hideKeyboardButton.setOnClickListener {
                service.requestHideSelf(0)
            }
        }
    }

    private val candidateUi by lazy {
        KawaiiBarUi.Candidate(context, horizontalCandidate.view)
    }

    private val titleUi by lazy { KawaiiBarUi.Title(context) }

    val barStateMachine =
        eventStateMachine<KawaiiBarState, KawaiiBarTransitionEvent>(Idle) {
            from(Idle) transitTo Title on ExtendedWindowAttached
            from(Idle) transitTo Candidate on PreeditUpdatedNonEmpty
            from(Title) transitTo Idle on WindowDetached
            from(Candidate) transitTo Idle on PreeditUpdatedEmpty

            onNewState {
                switchUiByState(it)
            }
        }

    val expandButtonStateMachine =
        eventStateMachine<ExpandButtonState, ExpandButtonTransitionEvent>(Hidden) {
            from(Hidden) transitTo ClickToAttachWindow on ExpandedCandidatesUpdatedNonEmpty
            from(ClickToAttachWindow) transitTo Hidden on ExpandedCandidatesUpdatedEmpty
            from(ClickToAttachWindow) transitTo ClickToDetachWindow on ExpandedCandidatesAttached
            from(ClickToDetachWindow) transitTo ClickToAttachWindow on ExpandedCandidatesDetachedWithNonEmpty
            from(ClickToDetachWindow) transitTo Hidden on ExpandedCandidatesDetachedWithEmpty

            onNewState {
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

        }

    val idleUiStateMachine =
        eventStateMachine<IdleUiState, IdleUiTransitionEvent>(Empty) {
            from(Toolbar) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Toolbar) transitTo Clipboard on MenuButtonClickedWithClipboardNonEmpty
            from(Toolbar) transitTo Empty on MenuButtonClickedWithClipboardEmpty
            from(Clipboard) transitTo Toolbar on MenuButtonClickedWithClipboardNonEmpty
            from(Clipboard) transitTo Empty on Timeout
            from(Clipboard) transitTo Empty on Pasted
            from(Empty) transitTo Toolbar on MenuButtonClickedWithClipboardEmpty
            from(Empty) transitTo Clipboard on ClipboardUpdatedNonEmpty

            onNewState {
                idleUi.switchUiByState(it)
            }
        }

    // set expand candidate button to create expand candidate
    private fun setExpandButtonToAttach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.attachWindow(ExpandedCandidateWindow())
        }
        candidateUi.expandButton.imageResource =
            R.drawable.ic_baseline_expand_more_24
    }

    // set expand candidate button to close expand candidate
    private fun setExpandButtonToDetach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.switchToKeyboardWindow()
        }
        candidateUi.expandButton.imageResource = R.drawable.ic_baseline_expand_less_24
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
    }

    private fun switchUiByState(state: KawaiiBarState) {
        val index = state.ordinal
        if (view.displayedChild == index)
            return
        Timber.i("Switch bar to $state")
        if (view.getChildAt(index) != titleUi.root) {
            titleUi.setReturnButtonOnClickListener { }
            titleUi.setTitle("")
            titleUi.removeExtension()
        }
        view.displayedChild = index
    }

    override val view by lazy {
        ViewAnimator(context).apply {
            add(idleUi.root, lParams(matchParent, matchParent))
            add(candidateUi.root, lParams(matchParent, matchParent))
            add(titleUi.root, lParams(matchParent, matchParent))
        }
    }


    override fun onPreeditUpdate(content: PreeditContent) {
        barStateMachine.push(
            if (content.preedit.preedit.isEmpty() && content.preedit.clientPreedit.isEmpty())
                PreeditUpdatedEmpty
            else
                PreeditUpdatedNonEmpty
        )
    }


    override fun onWindowAttached(window: InputWindow) {
        when (window) {
            is InputWindow.ExtendedInputWindow<*> -> {
                titleUi.setTitle(window.title)
                window.barExtension?.let { titleUi.addExtension(it) }
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

    companion object {
        @Volatile
        private var timeoutJobId = 0
    }
}