package org.fcitx.fcitx5.android.input

import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.text.InputType
import android.view.*
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.service.FcitxDaemonManager
import org.fcitx.fcitx5.android.utils.inputConnection
import splitties.bitflags.hasFlag
import timber.log.Timber

class FcitxInputMethodService : LifecycleInputMethodService() {

    @JvmInline
    value class CursorRange private constructor(val data: IntArray) {
        constructor(start: Int = 0, end: Int = 0) : this(intArrayOf(start, end))

        val start: Int get() = data[0]
        val end: Int get() = data[1]

        fun isEmpty() = data[0] == data[1]
        fun isNotEmpty() = data[0] != data[1]

        fun clear() {
            data[0] = 0
            data[1] = 0
        }

        fun update(start: Int, end: Int) {
            if (end >= start) {
                data[0] = start
                data[1] = end
            } else {
                data[0] = end
                data[1] = start
            }
        }

        fun update(i: Int) {
            data[0] = i
            data[1] = i
        }

        fun contains(other: CursorRange): Boolean {
            return start <= other.start && other.end <= end
        }
    }

    private lateinit var inputView: InputView
    private lateinit var fcitx: Fcitx
    private var eventHandlerJob: Job? = null
    private var fcitxDelayedTask: (suspend () -> Unit)? = null

    var editorInfo: EditorInfo? = null

    private var cursorAnchorAvailable = false

    val selection = CursorRange()
    val composing = CursorRange()
    private var composingText = ""
    private var fcitxCursor = -1

    private val ignoreSystemCursor by AppPrefs.getInstance().advanced.ignoreSystemCursor

    private val onThemeChangedListener = ThemeManager.OnThemeChangedListener {
        // InputView should be created first in onCreateInputView
        // setInputView should be used to 'replace' current InputView only
        if (!::inputView.isInitialized) return@OnThemeChangedListener
        createInputView(it)
        setInputView(inputView)
    }

    override fun onCreate() {
        FcitxDaemonManager.bindFcitxDaemon(javaClass.name, this) {
            fcitx = getFcitxDaemon().fcitx
            onReady {
                fcitxDelayedTask?.invoke()
                fcitxDelayedTask = null
            }
        }
        ThemeManager.addOnChangedListener(onThemeChangedListener)
        super.onCreate()
    }

    private fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CommitStringEvent -> {
                inputConnection?.commitText(event.data, 1)
                // committed text should be put before start of composing (if any), or before cursor
                val start = if (composing.isEmpty()) selection.start else composing.start
                selection.update(start + event.data.length)
                // clear composing range, but retain it's position for next update
                // see [^1] for explanation
                composing.update(selection.start)
            }
            is FcitxEvent.KeyEvent -> event.data.also {
                if (it.states.virtual) {
                    // KeyEvent from virtual keyboard
                    when (it.unicode) {
                        '\b'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                        '\r'.code -> handleReturnKey()
                        else -> inputConnection?.commitText(Char(it.unicode).toString(), 1)
                    }
                } else {
                    // KeyEvent from hardware keyboard (or input method engine forwardKey)
                    val keyCode = it.sym.keyCode
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        // recognized keyCode
                        val eventTime = SystemClock.uptimeMillis()
                        if (it.up) {
                            sendUpKeyEvent(eventTime, keyCode, it.states.metaState)
                        } else {
                            sendDownKeyEvent(eventTime, keyCode, it.states.metaState)
                        }
                    } else {
                        // no matching keyCode, commit character once on key down
                        if (!it.up && it.unicode > 0) {
                            inputConnection?.commitText(Char(it.unicode).toString(), 1)
                        } else {
                            Timber.w("Unhandled Fcitx KeyEvent: $it")
                        }
                    }
                }
            }
            is FcitxEvent.PreeditEvent -> event.data.let {
                updateComposingText(it.clientPreedit, it.clientCursor)
            }
            else -> {
            }
        }
        inputView.handleFcitxEvent(event)
    }

    private fun handleReturnKey() {
        editorInfo?.apply {
            if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                return
            }
            if (actionLabel?.isNotEmpty() == true && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConnection?.performEditorAction(actionId)
                return
            }
            if (imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                inputConnection?.commitText("\n", 1)
                return
            }
            when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_UNSPECIFIED,
                EditorInfo.IME_ACTION_NONE -> inputConnection?.commitText("\n", 1)
                else -> inputConnection?.performEditorAction(action)
            }
        } ?: sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    private fun sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        inputConnection?.sendKeyEvent(
            KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyEventCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    private fun sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        inputConnection?.sendKeyEvent(
            KeyEvent(
                eventTime,
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyEventCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    fun sendCombinationKeyEvents(
        keyEventCode: Int,
        alt: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false
    ) {
        inputConnection?.beginBatchEdit() ?: return
        var metaState = 0
        if (alt) metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (ctrl) metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        val eventTime = SystemClock.uptimeMillis()
        if (alt) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
        if (ctrl) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (shift) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        sendDownKeyEvent(eventTime, keyEventCode, metaState)
        sendUpKeyEvent(eventTime, keyEventCode, metaState)
        if (shift) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        if (ctrl) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (alt) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
        inputConnection?.endBatchEdit()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch {
            fcitx.reset()
        }
    }

    override fun onCreateInputView(): View {
        super.onCreateInputView()
        createInputView()
        return inputView
    }

    private fun createInputView(theme: Theme = ThemeManager.getActiveTheme()) {
        if (eventHandlerJob == null)
            eventHandlerJob = fcitx
                .eventFlow
                .onEach { handleFcitxEvent(it) }
                .launchIn(lifecycleScope)
        if (::inputView.isInitialized)
            inputView.scope.clear()
        inputView = InputView(this, fcitx, theme)
    }

    override fun setInputView(view: View) {
        window.window!!.decorView
            .findViewById<FrameLayout>(android.R.id.inputArea)
            .updateLayoutParams<ViewGroup.LayoutParams> {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        super.setInputView(view)
        view.updateLayoutParams<ViewGroup.LayoutParams> {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onComputeInsets(outInsets: Insets) {
        if (!this::inputView.isInitialized) return
        val (_, y) = intArrayOf(0, 0).also { inputView.keyboardView.getLocationInWindow(it) }
        outInsets.apply {
            contentTopInsets = y
            touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
            touchableRegion.setEmpty()
            visibleTopInsets = y
        }
    }

    override fun onEvaluateFullscreenMode() = false

    private fun forwardKeyEvent(event: KeyEvent, up: Boolean = false): Boolean {
        val states = KeyStates.fromKeyEvent(event)
        val charCode = event.unicodeChar
        // try send charCode first, allow upper case and lower case character
        // generating different KeySym
        // skip \n, because fcitx wants \r for return
        if (charCode > 0 && charCode != '\n'.code) {
            lifecycleScope.launch { fcitx.sendKey(charCode.toUInt(), states.states, up) }
            return true
        }
        val keySym = KeySym.fromKeyEvent(event)
        if (keySym != null) {
            lifecycleScope.launch { fcitx.sendKey(keySym, states, up) }
            return true
        }
        Timber.d("Skipped KeyEvent: $event")
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return forwardKeyEvent(event, false) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return forwardKeyEvent(event, true) || super.onKeyUp(keyCode, event)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        // update selection as soon as possible
        selection.update(attribute.initialSelStart, attribute.initialSelEnd)
        composing.clear()
        composingText = ""
        editorInfo = attribute
        Timber.d("onStartInput: initialSel=$selection")
        if (!restarting) {
            inputConnection?.apply {
                cursorAnchorAvailable = requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
            }
        }
        // fcitx might not be initialized yet, so we do setCapFlags later
        val setCapFlags = suspend {
            fcitx.setCapFlags(CapabilityFlags.fromEditorInfo(editorInfo))
        }
        if (::fcitx.isInitialized)
            lifecycleScope.launch { setCapFlags() }
        else
            fcitxDelayedTask = setCapFlags
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        lifecycleScope.launch {
            if (restarting) {
                // when input restarts in the same editor, unfocus it to clear previous state
                fcitx.focus(false)
            }
            fcitx.focus()
        }
        inputView.onShow()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        // onUpdateSelection can left behind when user types quickly enough, eg. long press backspace
        // TODO: call InputConnection#beginBatchEdit() before starting key repeat
        // skip cursor update if we are already up-to-date
        if (selection.start == newSelStart && selection.end == newSelEnd) return
        selection.update(newSelStart, newSelEnd)
        Timber.d("onUpdateSelection: $selection")
        // since `CursorAnchorInfo` has composingText, we can determine whether it's up-to-date
        // handle cursor update in `onUpdateSelection` only when `CursorAnchorInfo` unavailable
        if (!cursorAnchorAvailable) {
            handleCursorUpdate()
        }
        if (this::inputView.isInitialized) {
            inputView.onSelectionUpdate(newSelStart, newSelEnd)
        }
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo) {
        // onUpdateCursorAnchorInfo can left behind when user types quickly enough
        // skip the event if `info.composingText` is not up-to-date.
        if (info.composingText?.contentEquals(composingText) != true) return
        selection.update(info.selectionStart, info.selectionEnd)
        Timber.d("onUpdateCursorAnchorInfo: selection=$selection")
        handleCursorUpdate()
    }

    private fun handleCursorUpdate() {
        // skip fcitx cursor move when composing empty
        if (composing.isEmpty()) return
        // workaround some misbehaved editors: report composingText but wrong selectionStart
        if (selection.start < 0 && composing.start >= 0) return
        Timber.d("handleCursorUpdate: composing=$composing selection=$selection")
        // check if cursor inside composing text
        if (composing.contains(selection)) {
            if (ignoreSystemCursor) return
            // fcitx cursor position is relative to client preedit (composing text)
            val position = selection.start - composing.start
            // cursor in InvokeActionEvent counts by 'char'
            val codePointPosition = composingText.codePointCount(0, position)
            // move fcitx cursor when cursor position changed
            if (codePointPosition != fcitxCursor) {
                Timber.d("handleCursorUpdate: move fcitx cursor to $codePointPosition")
                lifecycleScope.launch {
                    fcitx.moveCursor(codePointPosition)
                }
            }
        } else {
            Timber.d("handleCursorUpdate: focus out/in")
            // cursor outside composing range, finish composing as-is
            inputConnection?.finishComposingText()
            // `fcitx.reset()` here would commit preedit after new cursor position
            // since we have `ClientUnfocusCommit`, focus out and in would do the trick
            lifecycleScope.launch {
                fcitx.focus(false)
                fcitx.focus()
            }
        }
    }

    // FIXME: cursor flicker
    // because setComposingText(text, cursor) can only put cursor at end of composing,
    // sometimes onUpdateCursorAnchorInfo/onUpdateSelection would receive event with wrong cursor position.
    // those events need to be filtered.
    // because of https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r45/core/java/android/view/inputmethod/BaseInputConnection.java#851
    // it's not possible to set cursor inside composing text
    private fun updateComposingText(text: String, cursor: Int) {
        fcitxCursor = cursor
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        do {
            if (text != composingText) {
                composingText = text
                // set composing text AND put cursor at end of composing
                ic.setComposingText(text, 1)
                if (text.isEmpty()) {
                    // clear composing text, put cursor at start of original composing
                    // [^1]: if this happens after committing text, composing should be cleared,
                    //       and selection should be put at original composing start. so composing
                    //       shouldn't be reset to [0, 0], but it's original position
                    selection.update(composing.start)
                    composing.clear()
                } else {
                    // update composing text, put cursor at end of new composing
                    val start = if (composing.isEmpty()) selection.start else composing.start
                    composing.update(start, start + text.length)
                    selection.update(composing.end)
                }
                Timber.d("updateComposingText: '$text' composing=$composing selection=$selection")
                if (cursor == text.length) {
                    // cursor already at end of composing, skip cursor reposition
                    break
                }
            }
            // skip cursor reposition when:
            // - user chose to ignore system cursor
            // - fcitx cursor position is invalid
            // - current and incoming composing text are both empty
            if (ignoreSystemCursor || (cursor < 0) || text.isEmpty()) break
            // fcitx cursor position is relative to client preedit (composing text)
            val p = cursor + composing.start
            if (p != selection.start) {
                Timber.d("updateComposingText: set Android selection ($p, $p)")
                ic.setSelection(p, p)
                selection.update(p)
            }
        } while (false)
        ic.endBatchEdit()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inputConnection?.finishComposingText()
        lifecycleScope.launch {
            fcitx.focus(false)
        }
    }

    override fun onFinishInput() {
        if (cursorAnchorAvailable) {
            cursorAnchorAvailable = false
            inputConnection?.requestCursorUpdates(0)
        }
        editorInfo = null
        lifecycleScope.launch {
            fcitx.setCapFlags(CapabilityFlags.DefaultFlags)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (this::fcitx.isInitialized && fcitx.isReady)
            runBlocking {
                fcitx.save()
            }
        return false
    }

    override fun onDestroy() {
        FcitxDaemonManager.unbind(javaClass.name)
        ThemeManager.removeOnChangedListener(onThemeChangedListener)
        eventHandlerJob?.cancel()
        eventHandlerJob = null
        super.onDestroy()
    }

    companion object {
        val isBoundToFcitxDaemon: Boolean
            get() = FcitxDaemonManager.hasConnection(FcitxInputMethodService::javaClass.name)
    }
}