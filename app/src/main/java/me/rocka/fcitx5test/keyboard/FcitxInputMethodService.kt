package me.rocka.fcitx5test.keyboard

import android.content.ServiceConnection
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.AppSharedPreferences
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.inputConnection
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import splitties.systemservices.inputMethodManager

class FcitxInputMethodService : InputMethodService(),
    CoroutineScope by MainScope() + SupervisorJob() {

    private lateinit var inputView: InputView
    private lateinit var fcitx: Fcitx
    private var eventHandlerJob: Job? = null
    private var connection: ServiceConnection? = null

    // `-1` means invalid, or don't know yet
    private var selectionStart = -1
    private var composingTextStart = -1
    private var composingText = ""
    private var fcitxCursor = -1

    override fun onCreate() {
        setTheme(R.style.Theme_AppCompat_DayNight)
        connection = bindFcitxDaemon {
            fcitx = getFcitxInstance()
        }
        super.onCreate()
    }

    private fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CommitStringEvent -> {
                inputConnection?.commitText(event.data, 1)
            }
            is FcitxEvent.KeyEvent -> event.data.let {
                if (Character.isISOControl(it.code)) {
                    when (it.code) {
                        '\b'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                        '\r'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                        else -> Log.d("KeyEvent", it.toString())
                    }
                } else {
                    sendKeyChar(Char(it.code))
                }
            }
            is FcitxEvent.PreeditEvent -> event.data.let {
                updateComposingTextWithCursor(it.clientPreedit, it.cursor)
            }
            else -> {}
        }
        inputView.handleFcitxEvent(event)
    }

    private fun onAction(v: View, it: KeyAction<*>, long: Boolean) {
        when (it) {
            is KeyAction.CommitAction -> {
                fcitx.reset()
                inputConnection?.commitText(it.act, 1)
            }
            is KeyAction.InputMethodSwitchAction -> {
                inputMethodManager.showInputMethodPicker()
            }
            else -> {}
        }
    }

    override fun onCreateInputView(): View {
        if (eventHandlerJob == null) {
            eventHandlerJob = fcitx.eventFlow.onEach {
                handleFcitxEvent(it)
            }.launchIn(this)
        }
        inputView = InputView(this, fcitx) { v, it, long -> onAction(v, it, long) }
        return inputView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        fcitx.reset()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        inputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }

    // FIXME: cursor flicker
    // because setComposingText(text, cursor) can only put cursor at end of composing,
    // sometimes onUpdateCursorAnchorInfo would receive event with wrong cursor position.
    // those events need to be filtered.
    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        if (AppSharedPreferences.getInstance().ignoreSystemCursor) return
        if (info == null) return
        selectionStart = info.selectionStart
        composingTextStart = info.composingTextStart
        Log.d("IMS", "AnchorInfo: selStart=$selectionStart cmpStart=$composingTextStart")
        info.composingText?.let { composing ->
            // check if cursor inside composing text
            if ((composingTextStart <= selectionStart) and
                (selectionStart <= composingTextStart + composing.length)
            ) {
                val position = selectionStart - composingTextStart
                // move fcitx cursor when:
                // - cursor position changed
                // - cursor position in composing text range; when user long press backspace key,
                //   onUpdateCursorAnchorInfo can be left behind, thus position is invalid.
                if ((position != fcitxCursor) and (position <= composingText.length)) {
                    fcitx.moveCursor(position)
                    return
                }
            }
            // TODO: maybe pass delete key directly when cursor outside of composing
        }
    }

    // because of https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r45/core/java/android/view/inputmethod/BaseInputConnection.java#851
    // it's not possible to set cursor inside composing text
    fun updateComposingTextWithCursor(text: String, cursor: Int) {
        fcitxCursor = cursor
        inputConnection?.run {
            if (text != composingText) {
                composingText = text
                // set composing text AND put cursor at end of composing
                setComposingText(text, 1)
                if (cursor == text.length) {
                    // cursor already at end of composing, skip cursor reposition
                    return
                }
            }
            if (AppSharedPreferences.getInstance().ignoreSystemCursor || (cursor < 0)) return
            // when user starts typing and there is no composing text, composingTextStart would be -1
            val p = cursor + composingTextStart
            Log.d("IMS", "TextWithCursor: p=$p composingStart=$composingTextStart")
            if (p != selectionStart) {
                Log.d("IMS", "TextWithCursor: move cursor $p")
                selectionStart = p
                setSelection(p, p)
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inputConnection?.requestCursorUpdates(0)
        fcitx.reset()
    }

    override fun onDestroy() {
        eventHandlerJob?.cancel()
        eventHandlerJob = null
        connection?.let { unbindService(it) }
        connection = null
        super.onDestroy()
    }
}
