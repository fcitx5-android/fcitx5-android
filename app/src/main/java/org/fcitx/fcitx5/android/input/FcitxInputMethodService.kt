package org.fcitx.fcitx5.android.input

import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.service.FcitxDaemonManager
import org.fcitx.fcitx5.android.utils.inputConnection
import splitties.bitflags.hasFlag
import timber.log.Timber

class FcitxInputMethodService : LifecycleInputMethodService() {

    data class SelectionInfo(val start: Int, val end: Int)

    private lateinit var inputView: InputView
    private lateinit var fcitx: Fcitx
    private var eventHandlerJob: Job? = null

    var editorInfo: EditorInfo? = null
    var selectionInfo = SelectionInfo(-1, -1)

    private var keyRepeatingJobs = hashMapOf<String, Job>()

    // `-1` means invalid, or don't know yet
    private var selectionStart = -1
    private var composingTextStart = -1
    private var composingText = ""
    private var fcitxCursor = -1

    private val ignoreSystemCursor by Prefs.getInstance().ignoreSystemCursor

    override fun onCreate() {
        FcitxDaemonManager.bindFcitxDaemon(javaClass.name, this) {
            fcitx = getFcitxDaemon().fcitx
        }
        super.onCreate()
    }

    private fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CommitStringEvent -> {
                inputConnection?.commitText(event.data, 1)
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
                updateComposingTextWithCursor(it.clientPreedit, it.clientCursor)
            }
            else -> {
            }
        }
        inputView.handleFcitxEvent(event)
    }

    private fun handleReturnKey() {
        if (editorInfo == null || editorInfo!!.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            return
        }
        editorInfo!!.run {
            if (actionLabel?.isNotEmpty() == true) {
                actionId
            } else {
                imeOptions and EditorInfo.IME_MASK_ACTION
            }.let { inputConnection?.performEditorAction(it) }
        }
    }

    fun startRepeating(key: String) {
        if (keyRepeatingJobs.containsKey(key)) {
            return
        }
        keyRepeatingJobs[key] = lifecycleScope.launch {
            while (true) {
                fcitx.sendKey(key)
                delay(60L)
            }
        }
    }

    fun cancelRepeating(key: String) {
        keyRepeatingJobs.run {
            get(key)?.cancel()
            remove(key)
        }
    }

    private fun cancelRepeatingAll() {
        keyRepeatingJobs.forEach { cancelRepeating(it.key) }
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
        if (eventHandlerJob == null)
            eventHandlerJob = fcitx
                .eventFlow
                .onEach { handleFcitxEvent(it) }
                .launchIn(lifecycleScope)
        inputView = InputView(this, fcitx)
        return inputView
    }

    override fun onEvaluateFullscreenMode() = false

    private fun forwardKeyEvent(event: KeyEvent, up: Boolean = false): Boolean {
        val states = KeyStates.fromKeyEvent(event)
        val charCode = event.unicodeChar
        // try send charCode first, allow upper case and lower case character
        // generating different KeySym
        if (charCode > 0) {
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event)
        }
        return forwardKeyEvent(event, false)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyUp(keyCode, event)
        }
        return forwardKeyEvent(event, true)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        inputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
        editorInfo = attribute
        lifecycleScope.launch {
            fcitx.setCapFlags(CapabilityFlags.fromEditorInfo(editorInfo))
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
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
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        selectionInfo = SelectionInfo(newSelStart, newSelEnd)
        if (::inputView.isInitialized)
            inputView.onSelectionUpdate(newSelStart, newSelEnd)
        else
            Timber.w("Ignore onSelectionUpdate: inputView is not initialized")
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        if (info == null) return
        selectionStart = info.selectionStart
        composingTextStart = info.composingTextStart
        Timber.d("AnchorInfo: selStart=$selectionStart cmpStart=$composingTextStart")
        val composing = info.composingText ?: return
        // workaround some misbehaved editors: report composingText but wrong selectionStart
        if (selectionStart < 0) return
        // check if cursor inside composing text
        if ((composingTextStart <= selectionStart) &&
            (selectionStart <= composingTextStart + composing.length)
        ) {
            if (!ignoreSystemCursor) {
                val position = selectionStart - composingTextStart
                // move fcitx cursor when:
                // - cursor position changed
                // - cursor position in composing text range; when user long press backspace key,
                //   onUpdateCursorAnchorInfo can be left behind, thus position is invalid.
                if ((position != fcitxCursor) && (position <= composingText.length)) {
                    lifecycleScope.launch {
                        fcitx.moveCursor(position)
                    }
                }
            }
        } else {
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
    // sometimes onUpdateCursorAnchorInfo would receive event with wrong cursor position.
    // those events need to be filtered.
    // because of https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r45/core/java/android/view/inputmethod/BaseInputConnection.java#851
    // it's not possible to set cursor inside composing text
    private fun updateComposingTextWithCursor(text: String, cursor: Int) {
        fcitxCursor = cursor
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        do {
            if (text != composingText) {
                composingText = text
                // set composing text AND put cursor at end of composing
                ic.setComposingText(text, 1)
                if (cursor == text.length) {
                    // cursor already at end of composing, skip cursor reposition
                    break
                }
            }
            if (ignoreSystemCursor || (cursor < 0)) break
            // when user starts typing and there is no composing text, composingTextStart would be -1
            val p = cursor + composingTextStart
            Timber.d("TextWithCursor: p=$p composingStart=$composingTextStart")
            if (p != selectionStart) {
                Timber.d("TextWithCursor: move cursor $p")
                selectionStart = p
                ic.setSelection(p, p)
            }
        } while (false)
        ic.endBatchEdit()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        cancelRepeatingAll()
        inputConnection?.finishComposingText()
        lifecycleScope.launch {
            fcitx.focus(false)
        }
    }

    override fun onFinishInput() {
        inputConnection?.requestCursorUpdates(0)
        editorInfo = null
        lifecycleScope.launch {
            fcitx.setCapFlags(CapabilityFlags.DefaultFlags)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("onUnbind")
        if (this::fcitx.isInitialized && fcitx.isReady)
            runBlocking {
                fcitx.save()
            }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        FcitxDaemonManager.unbind(javaClass.name)
        eventHandlerJob?.cancel()
        eventHandlerJob = null
        super.onDestroy()
    }

    companion object {
        val isBoundToFcitxDaemon: Boolean
            get() = FcitxDaemonManager.hasConnection(FcitxInputMethodService::javaClass.name)
    }
}