package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
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

    override fun onCreate() {
        bindFcitxDaemon { binder ->
            fcitx = binder.getFcitxInstance()
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
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        fcitx.reset()
    }

    override fun onFinishInput() {
        fcitx.reset()
    }

    override fun onDestroy() {
        eventHandlerJob?.cancel()
        super.onDestroy()
    }
}
