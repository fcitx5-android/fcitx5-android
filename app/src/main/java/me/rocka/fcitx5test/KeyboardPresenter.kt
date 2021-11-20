package me.rocka.fcitx5test

import android.util.Log
import android.view.KeyEvent
import me.rocka.fcitx5test.KeyboardContract.CapsState
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import java.util.*
import kotlin.concurrent.timer

class KeyboardPresenter(
    val service: FcitxService,
    val view: KeyboardContract.View,
    override val fcitx: Fcitx,
) :
    KeyboardContract.Presenter {

    override var capsState: CapsState = CapsState.None

    private var backspaceTimer: Timer? = null

    private var cachedPreedit = KeyboardContract.PreeditContent(
        FcitxEvent.PreeditEvent.Data("", ""),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )

    override fun selectCandidate(idx: Int) {
        fcitx.select(idx)
    }

    override fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CandidateListEvent -> view.updateCandidates(event.data)
            is FcitxEvent.CommitStringEvent -> service.currentInputConnection?.commitText(
                event.data,
                1
            )
            is FcitxEvent.IMChangeEvent -> view.updateLangSwitchButtonText(event.data.status.label)
            is FcitxEvent.InputPanelAuxEvent -> {
                cachedPreedit.aux = event.data
                view.updatePreedit(cachedPreedit)
            }
            is FcitxEvent.KeyEvent -> {
                if (Character.isISOControl(event.data.code)) {
                    when (event.data.code) {
                        '\b'.code -> service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                        '\r'.code -> service.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                        else -> Log.d("KeyEvent", event.data.toString())
                    }
                } else {
                    service.sendKeyChar(Char(event.data.code))
                }
            }
            is FcitxEvent.PreeditEvent -> {
                cachedPreedit.preedit = event.data
                view.updatePreedit(cachedPreedit)
                service.currentInputConnection?.setComposingText(event.data.clientPreedit, 1)
            }
            is FcitxEvent.ReadyEvent -> {
                view.updateLangSwitchButtonText(fcitx.imeStatus().label)
            }
            is FcitxEvent.UnknownEvent -> {}
        }
    }

    override fun switchLang() {
        val list = fcitx.listIme()
        if (list.isEmpty()) return
        val status = fcitx.imeStatus()
        val index = list.indexOfFirst { it.uniqueName == status.uniqueName }
        val next = list[(index + 1) % list.size]
        fcitx.setIme(next.uniqueName)
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
        when (fcitx.imeStatus().label) {
            "us" -> 'e'
            else -> 'z'
        }.also { fcitx.sendKey(it) }
    }
}