package org.fcitx.fcitx5.android.input.bar

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionsResponse
import android.widget.FrameLayout
import android.widget.ViewAnimator
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.CandidateEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.PreeditEmpty
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
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private val expandToolbarByDefault by AppPrefs.getInstance().keyboard.expandToolbarByDefault
    private val toolbarNumRowOnPassword by AppPrefs.getInstance().keyboard.toolbarNumRowOnPassword

    private var clipboardTimeoutJob: Job? = null

    private var isClipboardEmpty: Boolean = true
    private var isInlineSuggestionEmpty: Boolean = true
    private var isCapabilityFlagsPassword: Boolean = false
    private var isKeyboardLayoutNumber: Boolean = false

    private val onClipboardUpdateListener =
        ClipboardManager.OnClipboardUpdateListener {
            if (!clipboardSuggestion.getValue()) return@OnClipboardUpdateListener
            service.lifecycleScope.launch {
                if (it.text.isEmpty()) {
                    isClipboardEmpty = true
                    evalIdleUiState()
                } else {
                    idleUi.setClipboardItemText(it.text.take(42))
                    isClipboardEmpty = false
                    evalIdleUiState()
                    launchClipboardTimeoutJob()
                }
            }
        }

    private val onClipboardSuggestionUpdateListener =
        ManagedPreference.OnChangeListener<Boolean> { _, it ->
            if (!it) {
                isClipboardEmpty = true
                evalIdleUiState()
                clipboardTimeoutJob?.cancel()
                clipboardTimeoutJob = null
            }
        }

    private val onClipboardTimeoutUpdateListener =
        ManagedPreference.OnChangeListener<Int> { _, _ ->
            when (idleUi.currentState) {
                KawaiiBarUi.Idle.State.Clipboard -> {
                    // renew timeout when clipboard suggestion is present
                    launchClipboardTimeoutJob()
                }
                else -> {}
            }
        }

    private val popupActionListener by lazy {
        popup.listener
    }

    private fun launchClipboardTimeoutJob() {
        clipboardTimeoutJob?.cancel()
        val timeout = clipboardItemTimeout.getValue() * 1000L
        // never transition to ClipboardTimedOut state when timeout < 0
        if (timeout < 0L) return
        clipboardTimeoutJob = service.lifecycleScope.launch {
            delay(timeout)
            isClipboardEmpty = true
            evalIdleUiState()
            clipboardTimeoutJob = null
        }
    }

    private fun evalIdleUiState() {
        val newState = when {
            !isClipboardEmpty -> KawaiiBarUi.Idle.State.Clipboard
            !isInlineSuggestionEmpty ->
                KawaiiBarUi.Idle.State.InlineSuggestion
            isCapabilityFlagsPassword && !isKeyboardLayoutNumber ->
                KawaiiBarUi.Idle.State.NumberRow
            else -> KawaiiBarUi.Idle.State.Empty
        }
        if (newState == idleUi.currentState)
            return
        else if (idleUi.currentState != KawaiiBarUi.Idle.State.Empty
            && newState == KawaiiBarUi.Idle.State.Empty
        ) {
            if (expandToolbarByDefault || idleUi.isToolbarExpanded) {
                idleUi.updateState(newState)
                idleUi.expandToolbar()
            } else
                idleUi.updateState(newState)
        } else idleUi.updateState(newState)
    }

    private val idleUi: KawaiiBarUi.Idle by lazy {
        KawaiiBarUi.Idle(
            context,
            theme,
            popupActionListener,
            popup,
            commonKeyActionListener
        ).apply {
            menuButton.setOnClickListener {
                idleUi.toggleToolbar()
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
                isClipboardEmpty = true
                evalIdleUiState()
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
        }
    }

    private val candidateUi by lazy {
        KawaiiBarUi.Candidate(context, theme, horizontalCandidate.view)
    }

    private val titleUi by lazy { KawaiiBarUi.Title(context, theme) }

    val barStateMachine = KawaiiBarStateMachine.new {
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
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
        clipboardSuggestion.registerOnChangeListener(onClipboardSuggestionUpdateListener)
        clipboardItemTimeout.registerOnChangeListener(onClipboardTimeoutUpdateListener)
    }

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            idleUi.privateMode(info.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        }
        isCapabilityFlagsPassword = toolbarNumRowOnPassword && capFlags.has(CapabilityFlag.Password)
        isInlineSuggestionEmpty = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            idleUi.inlineSuggestionsBar.clear()
        }
        evalIdleUiState()
        if (idleUi.currentState == KawaiiBarUi.Idle.State.Empty && expandToolbarByDefault)
            idleUi.expandToolbar()
    }

    override fun onPreeditEmptyStateUpdate(empty: Boolean) {
        barStateMachine.push(PreeditUpdated, PreeditEmpty to empty)
    }

    override fun onCandidateUpdate(data: Array<String>) {
        barStateMachine.push(CandidatesUpdated, CandidateEmpty to data.isEmpty())
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
        barStateMachine.push(WindowDetached)
    }

    private val suggestionSize by lazy {
        Size(ViewGroup.LayoutParams.WRAP_CONTENT, context.dp(HEIGHT))
    }

    private val directExecutor by lazy {
        Executor { it.run() }
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.R)
    fun handleInlineSuggestions(response: InlineSuggestionsResponse): Boolean {
        if (response.inlineSuggestions.isEmpty()) {
            isInlineSuggestionEmpty = true
            return true
        }
        var pinned: InlineSuggestion? = null
        val scrollable = mutableListOf<InlineSuggestion>()
        var extraPinnedCount = 0
        response.inlineSuggestions.forEach {
            if (it.info.isPinned) {
                if (pinned == null) {
                    pinned = it
                } else {
                    scrollable.add(extraPinnedCount++, it)
                }
            } else {
                scrollable.add(it)
            }
        }
        service.lifecycleScope.launch {
            idleUi.inlineSuggestionsBar.setPinnedView(
                pinned?.let { inflateInlineContentView(it) }
            )
        }
        service.lifecycleScope.launch {
            val views = scrollable.map { s ->
                service.lifecycleScope.async {
                    inflateInlineContentView(s)
                }
            }.awaitAll()
            idleUi.inlineSuggestionsBar.setScrollableViews(views)
        }
        isInlineSuggestionEmpty = false
        evalIdleUiState()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun inflateInlineContentView(suggestion: InlineSuggestion): InlineContentView {
        return suspendCoroutine { c ->
            suggestion.inflate(context, suggestionSize, directExecutor) { v ->
                c.resume(v)
            }
        }
    }

    companion object {
        const val HEIGHT = 40
    }

    fun onKeyboardLayoutSwitched(isNumber: Boolean) {
        isKeyboardLayoutNumber = isNumber
        evalIdleUiState()
    }

}
