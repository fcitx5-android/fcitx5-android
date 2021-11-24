package me.rocka.fcitx5test.keyboard

import android.util.Log
import android.view.KeyEvent
import me.rocka.fcitx5test.keyboard.KeyboardContract.CapsState
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import java.util.*
import kotlin.concurrent.timer

class KeyboardPresenter(
    val service: FcitxInputMethodService,
    val view: KeyboardContract.View,
    override val fcitx: Fcitx,
) :
    KeyboardContract.Presenter {

    override var capsState: CapsState = CapsState.None

    private var backspaceTimer: Timer? = null

    private var cachedPreedit = KeyboardContract.PreeditContent(
        FcitxEvent.PreeditEvent.Data("", "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )

    override fun selectCandidate(idx: Int) {
        fcitx.select(idx)
    }

    override fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CandidateListEvent -> {
                view.updateCandidates(event.data)
            }
            is FcitxEvent.CommitStringEvent -> {
                service.currentInputConnection?.commitText(event.data, 1)
            }
            is FcitxEvent.IMChangeEvent -> {
                view.updateSpaceButtonText(event.data.status)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                cachedPreedit.aux = event.data
                view.updatePreedit(cachedPreedit)
            }
            is FcitxEvent.KeyEvent -> event.data.let {
                if (Character.isISOControl(it.code)) {
                    when (it.code) {
                        '\b'.code -> service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                        '\r'.code -> service.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                        else -> Log.d("KeyEvent", it.toString())
                    }
                } else {
                    service.sendKeyChar(Char(it.code))
                }
            }
            is FcitxEvent.PreeditEvent -> event.data.let {
                cachedPreedit.preedit = it
                view.updatePreedit(cachedPreedit)
                service.updateComposingTextWithCursor(it.clientPreedit, it.cursor)
            }
            is FcitxEvent.ReadyEvent -> {
                fcitx.ime().let { view.updateSpaceButtonText(it) }
            }
            is FcitxEvent.UnknownEvent -> {}
        }
    }

    override fun switchLang() {
        val list = fcitx.enabledIme()
        if (list.isEmpty()) return
        val status = fcitx.ime()
        val index = list.indexOfFirst { it.uniqueName == status.uniqueName }
        val next = list[(index + 1) % list.size]
        fcitx.activateIme(next.uniqueName)
    }

    override fun switchCapsState() {
        capsState = when (capsState) {
            CapsState.None -> CapsState.Once
            CapsState.Once -> CapsState.Lock
            CapsState.Lock -> CapsState.None
        }
    }

    override fun onKeyPress(key: Char) {
        var c = key
        when (capsState) {
            CapsState.None -> {
                c = c.lowercaseChar()
            }
            CapsState.Once -> {
                c = c.uppercaseChar()
                capsState = CapsState.None
            }
            CapsState.Lock -> {
                c = c.uppercaseChar()
            }
        }
        fcitx.sendKey(c)
    }

    override fun backspace() {
        fcitx.sendKey("BackSpace")
    }

    override fun startDeleting() {
        backspaceTimer = timer(period = 40L, action = { backspace() })
    }

    override fun stopDeleting() {
        backspaceTimer?.run { cancel(); purge() }
    }

    override fun space() {
        fcitx.sendKey("space")
    }

    override fun enter() {
        fcitx.sendKey("Return")
    }

    override fun quickPhrase() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
    }

    override fun punctuation() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
        when (fcitx.ime().label) {
            "En" -> 'e'
            else -> 'z'
        }.also { fcitx.sendKey(it) }
    }
}