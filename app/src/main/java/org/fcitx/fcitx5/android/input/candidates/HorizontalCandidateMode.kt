package org.fcitx.fcitx5.android.input.candidates

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class HorizontalCandidateMode {
    NeverFillWidth,
    AutoFillWidth,
    AlwaysFillWidth;

    companion object : ManagedPreference.StringLikeCodec<HorizontalCandidateMode> {
        override fun decode(raw: String): HorizontalCandidateMode = valueOf(raw)
    }
}