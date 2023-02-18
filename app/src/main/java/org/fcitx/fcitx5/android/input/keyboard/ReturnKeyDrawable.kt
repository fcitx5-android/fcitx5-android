package org.fcitx.fcitx5.android.input.keyboard

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import splitties.bitflags.hasFlag

object ReturnKeyDrawable {
    fun fromEditorInfo(info: EditorInfo): Int {
        if (info.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            return R.drawable.ic_baseline_keyboard_return_24
        }
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> R.drawable.ic_baseline_arrow_forward_24
            EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_baseline_search_24
            EditorInfo.IME_ACTION_SEND -> R.drawable.ic_baseline_send_24
            EditorInfo.IME_ACTION_NEXT -> R.drawable.ic_baseline_keyboard_tab_24
            EditorInfo.IME_ACTION_DONE -> R.drawable.ic_baseline_done_24
            EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.ic_baseline_keyboard_tab_reverse_24
            else -> R.drawable.ic_baseline_keyboard_return_24
        }
    }

    fun from(fcitx: FcitxConnection, info: EditorInfo): Int {
        val preedit = fcitx.runImmediately { inputPanelCached.preedit }
        val clientPreedit = fcitx.runImmediately { clientPreeditCached }
        return if (preedit.isEmpty() && clientPreedit.isEmpty()) {
            R.drawable.ic_baseline_keyboard_return_24
        } else {
            fromEditorInfo(info)
        }
    }
}
