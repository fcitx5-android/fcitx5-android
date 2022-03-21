package org.fcitx.fcitx5.android.input.candidates.expanded

import org.fcitx.fcitx5.android.data.Prefs

enum class ExpandedCandidateStyle {
    Grid,
    Flexbox;

    companion object : Prefs.StringLikeCodec<ExpandedCandidateStyle> {
        override fun decode(raw: String): ExpandedCandidateStyle? =
            runCatching { valueOf(raw) }.getOrNull()
    }
}