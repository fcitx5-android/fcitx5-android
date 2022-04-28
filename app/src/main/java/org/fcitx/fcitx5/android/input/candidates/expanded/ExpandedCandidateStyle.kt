package org.fcitx.fcitx5.android.input.candidates.expanded

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class ExpandedCandidateStyle {
    Grid,
    Flexbox;

    companion object : ManagedPreference.StringLikeCodec<ExpandedCandidateStyle> {
        override fun decode(raw: String): ExpandedCandidateStyle = valueOf(raw)
    }
}