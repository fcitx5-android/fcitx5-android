package me.rocka.fcitx5test.input.broadcast

import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.preedit.PreeditContent

interface InputBroadcastReceiver {

    fun onPreeditUpdate(content: PreeditContent) {}

    fun onImeUpdate(ime: InputMethodEntry) {}

    fun onCandidateUpdates(data: Array<String>){}

}