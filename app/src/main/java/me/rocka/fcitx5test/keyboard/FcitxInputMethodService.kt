package me.rocka.fcitx5test.keyboard

import android.content.ServiceConnection
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.registerSharedPerfChangeListener
import me.rocka.fcitx5test.settings.PreferenceKeys
import me.rocka.fcitx5test.unregisterSharedPerfChangeListener

class FcitxInputMethodService :
    InputMethodService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var keyboardPresenter: KeyboardPresenter
    private lateinit var keyboardView: KeyboardView
    private lateinit var fcitx: Fcitx
    private var eventHandlerJob: Job? = null
    private var connection: ServiceConnection? = null

    private var ignoreSystemCursor = true

    // `-1` means invalid, or don't know yet
    private var selectionStart = -1
    private var composingTextStart = -1
    private var composingText = ""
    private var fcitxCursor = -1

    override fun onCreate() {
        connection = bindFcitxDaemon {
            fcitx = getFcitxInstance()
        }
        registerSharedPerfChangeListener(this, PreferenceKeys.IgnoreSystemCursor)
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val preeditBinding = KeyboardPreeditBinding.inflate(layoutInflater)

        keyboardView = KeyboardView(this, preeditBinding)
        keyboardPresenter = KeyboardPresenter(this, keyboardView, fcitx)
        keyboardView.presenter = keyboardPresenter

        fcitx.ime().let { keyboardView.updateSpaceButtonText(it) }

        if (eventHandlerJob == null)
            eventHandlerJob = fcitx.eventFlow.onEach {
                keyboardPresenter.handleFcitxEvent(it)
            }.launchIn(MainScope())

        return keyboardView.keyboardView.root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        fcitx.reset()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        currentInputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }

    // FIXME: cursor flicker
    // because setComposingText(text, cursor) can only put cursor at end of composing,
    // sometimes onUpdateCursorAnchorInfo would receive event with wrong cursor position.
    // those events need to be filtered.
    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        if (ignoreSystemCursor) return
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
        currentInputConnection.run {
            if (text != composingText) {
                composingText = text
                // set composing text AND put cursor at end of composing
                setComposingText(text, 1)
                if (cursor == text.length) {
                    // cursor already at end of composing, skip cursor reposition
                    return
                }
            }
            if (ignoreSystemCursor or (cursor < 0)) return
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
        currentInputConnection.requestCursorUpdates(0)
        fcitx.reset()
    }

    override fun onDestroy() {
        eventHandlerJob?.cancel()
        eventHandlerJob = null
        connection?.let { unbindService(it) }
        connection = null
        unregisterSharedPerfChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key) {
            PreferenceKeys.IgnoreSystemCursor -> {
                ignoreSystemCursor = pref.getBoolean(key, true)
            }
        }
    }
}
