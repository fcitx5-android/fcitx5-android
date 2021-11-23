package me.rocka.fcitx5test

import android.content.ServiceConnection
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.databinding.QwertyKeyboardBinding
import me.rocka.fcitx5test.native.Fcitx

class FcitxIMEService : InputMethodService() {

    private lateinit var keyboardPresenter: KeyboardPresenter
    private lateinit var keyboardView: KeyboardView
    private lateinit var fcitx: Fcitx
    private var eventHandlerJob: Job? = null
    private var connection: ServiceConnection? = null

    // `-1` means invalid, or don't know yet
    private var selectionStart = -1
    private var composingTextStart = -1

    override fun onCreate() {
        connection = bindFcitxDaemon {
            fcitx = getFcitxInstance()
            eventHandlerJob = fcitx.eventFlow.onEach {
                keyboardPresenter.handleFcitxEvent(it)
            }.launchIn(MainScope())
        }
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val keyboardBinding = QwertyKeyboardBinding.inflate(layoutInflater)
        val preeditBinding = KeyboardPreeditBinding.inflate(layoutInflater)

        keyboardView = KeyboardView(this, keyboardBinding, preeditBinding)
        keyboardPresenter = KeyboardPresenter(this, keyboardView, fcitx)
        keyboardView.presenter = keyboardPresenter

        fcitx.ime().let { keyboardView.updateSpaceButtonText(it) }
        return keyboardView.keyboardBinding.root
    }

    // we don't create preedit view here, but in onCreateInputView.
    override fun onCreateCandidatesView(): View = keyboardView.preeditBinding.root

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        fcitx.reset()
        currentInputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        if (info == null) return
        selectionStart = info.selectionStart
        composingTextStart = info.composingTextStart
        info.composingText?.let { composing ->
            if ((info.composingTextStart <= info.selectionStart) and
                (info.selectionStart <= info.composingTextStart + composing.length)
            ) {
                val position = info.selectionStart - info.composingTextStart
                fcitx.moveCursor(position)
                return
            }
            // TODO: maybe pass delete key directly when cursor outside of composing
        }
    }

    // because of https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r45/core/java/android/view/inputmethod/BaseInputConnection.java#851
    // it's not possible to set cursor inside composing text
    fun updateComposingTextWithCursor(text: String, cursor: Int) {
        currentInputConnection.run {
            val p = cursor + if (composingTextStart >= 0) composingTextStart else selectionStart
            setComposingText(text, 1)
            if (cursor < 0) return
            setSelection(p, p)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        fcitx.reset()
    }

    override fun onFinishInput() {
        fcitx.reset()
    }

    override fun onDestroy() {
        eventHandlerJob?.cancel()
        connection?.let { unbindService(it) }
        super.onDestroy()
    }
}
