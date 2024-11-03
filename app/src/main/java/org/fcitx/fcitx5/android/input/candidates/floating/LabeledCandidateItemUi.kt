/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.floating

import android.content.Context
import android.graphics.Color
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.backgroundColor
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.textView
import splitties.views.setPaddingDp

class LabeledCandidateItemUi(
    override val ctx: Context,
    val theme: Theme,
    val textSize: Float = 16f // sp
) : Ui {

    override val root = textView {
        textSize = this@LabeledCandidateItemUi.textSize
        setPaddingDp(3, 1, 3, 1)
    }

    fun update(candidate: FcitxEvent.Candidate, active: Boolean) {
        val fg = if (active) theme.genericActiveForegroundColor else theme.keyTextColor
        val altFg = if (active) theme.genericActiveForegroundColor else theme.altKeyTextColor
        root.text = buildSpannedString {
            color(fg) { append(candidate.label) }
            color(fg) { append(candidate.text) }
            if (candidate.comment.isNotBlank()) {
                append(" ")
                color(altFg) { append(candidate.comment) }
            }
        }
        val bg = if (active) theme.genericActiveBackgroundColor else Color.TRANSPARENT
        root.backgroundColor = bg
    }
}
