package me.rocka.fcitx5test.input.preedit

import me.rocka.fcitx5test.core.FcitxEvent

data class PreeditContent(
    var preedit: FcitxEvent.PreeditEvent.Data,
    var aux: FcitxEvent.InputPanelAuxEvent.Data
)
