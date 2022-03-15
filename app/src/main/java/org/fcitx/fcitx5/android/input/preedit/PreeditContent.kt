package org.fcitx.fcitx5.android.input.preedit

import org.fcitx.fcitx5.android.core.FcitxEvent

data class PreeditContent(
    var preedit: FcitxEvent.PreeditEvent.Data,
    var aux: FcitxEvent.InputPanelAuxEvent.Data
)
