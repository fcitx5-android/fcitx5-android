package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class SpaceLongPressBehavior {
    None,
    Enumerate,
    ToggleActivate,
    ShowPicker;

    companion object : ManagedPreference.StringLikeCodec<SpaceLongPressBehavior> {
        override fun decode(raw: String): SpaceLongPressBehavior = valueOf(raw)
    }
}
