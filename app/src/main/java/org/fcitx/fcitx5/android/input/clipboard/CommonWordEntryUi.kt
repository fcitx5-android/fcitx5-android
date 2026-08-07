/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.alpha
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.setPaddingDp

class CommonWordEntryUi(
    override val ctx: Context,
    theme: Theme,
    radius: Float
) : Ui {
    private val phrase = textView {
        maxLines = 3
        textSize = 14f
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(theme.keyTextColor)
    }

    private val keyword = textView {
        isSingleLine = true
        textSize = 11f
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(theme.keyTextColor.alpha(0.55f))
    }

    private val content = verticalLayout {
        setPaddingDp(8, 4, 8, 4)
        add(phrase, lParams(matchParent, wrapContent))
        add(keyword, lParams(matchParent, wrapContent))
    }

    override val root = CustomGestureView(ctx).apply {
        isClickable = true
        foreground = RippleDrawable(
            ColorStateList.valueOf(theme.keyPressHighlightColor),
            null,
            GradientDrawable().apply {
                cornerRadius = radius
                setColor(Color.WHITE)
            }
        )
        background = GradientDrawable().apply {
            cornerRadius = radius
            setColor(theme.clipboardEntryColor)
        }
        add(content, lParams(matchParent, wrapContent))
    }

    fun setEntry(entry: QuickPhraseEntry) {
        phrase.text = ClipboardAdapter.excerptText(entry.phrase)
        keyword.text = entry.keyword
    }
}
