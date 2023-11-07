package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class LangSwitchBehavior {
    Enumerate,
    ToggleActivate,
    NextInputMethodApp;

    companion object : ManagedPreference.StringLikeCodec<LangSwitchBehavior> {
        override fun decode(raw: String): LangSwitchBehavior = valueOf(raw)
    }
}
