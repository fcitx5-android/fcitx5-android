package me.rocka.fcitx5test

import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import me.rocka.fcitx5test.native.InputMethodEntry

interface KeyboardContract {
    data class PreeditContent(
        var preedit: FcitxEvent.PreeditEvent.Data,
        var aux: FcitxEvent.InputPanelAuxEvent.Data
    )

    enum class CapsState { None, Once, Lock }

    interface View {

        fun updatePreedit(data: PreeditContent)

        fun updateCandidates(data: List<String>)

        fun updateSpaceButtonText(entry: InputMethodEntry)

    }

    interface Presenter {
        val fcitx: Fcitx

        var capsState: CapsState

        fun selectCandidate(idx: Int)

        fun handleFcitxEvent(event: FcitxEvent<*>)

        fun switchLang()

        fun switchCapsState()

        fun onKeyPress(key: Char)

        fun backspace()

        fun startDeleting()

        fun stopDeleting()

        fun space()

        fun enter()

        fun quickPhrase()

        fun punctuation()

    }
}