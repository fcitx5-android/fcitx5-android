package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.databinding.QwertyKeyboardBinding
import me.rocka.fcitx5test.native.Fcitx

class FcitxService : InputMethodService(), LifecycleOwner {

    private lateinit var fcitx: Fcitx
    private val dispatcher = ServiceLifecycleDispatcher(this)

    private lateinit var keyboardPresenter: KeyboardPresenter
    private lateinit var keyboardView: KeyboardView

    override fun getLifecycle(): Lifecycle {
        return dispatcher.lifecycle
    }

    override fun onCreate() {
        fcitx = Fcitx(this)
        lifecycle.addObserver(fcitx)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val keyboardBinding = QwertyKeyboardBinding.inflate(layoutInflater)
        val preeditBinding = KeyboardPreeditBinding.inflate(layoutInflater)

        keyboardView = KeyboardView(this, keyboardBinding, preeditBinding)
        keyboardPresenter = KeyboardPresenter(this, keyboardView, fcitx)
        keyboardView.presenter = keyboardPresenter

        fcitx.eventFlow.onEach {
            keyboardPresenter.handleFcitxEvent(it)
        }.launchIn(lifecycle.coroutineScope)

        return keyboardView.keyboardBinding.root
    }

    // we don't create preedit view here, but in onCreateInputView.
    override fun onCreateCandidatesView(): View = keyboardView.preeditBinding.root

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        fcitx.reset()
    }

    override fun onFinishInput() {
        fcitx.reset()
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
