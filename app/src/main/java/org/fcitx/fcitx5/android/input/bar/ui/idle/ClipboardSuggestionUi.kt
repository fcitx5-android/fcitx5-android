/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardSuggestion
import org.fcitx.fcitx5.android.data.clipboard.ClipboardSuggestionKind
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable

class ClipboardSuggestionUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val icon = imageView {
        imageDrawable = drawable(R.drawable.ic_clipboard)!!.apply {
            setTint(theme.altKeyTextColor)
        }
    }

    val text = textView {
        isSingleLine = true
        maxWidth = dp(120)
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(theme.altKeyTextColor)
    }

    private val layout = constraintLayout {
        val spacing = dp(4)
        add(icon, lParams(dp(20), dp(20)) {
            startOfParent(spacing)
            before(text)
            centerVertically()
        })
        add(text, lParams(wrapContent, wrapContent) {
            after(icon, spacing)
            endOfParent(spacing)
            centerVertically()
        })
    }

    val suggestionView = CustomGestureView(ctx).apply {
        add(layout, lParams(wrapContent, matchParent))
        background = rippleDrawable(theme.keyPressHighlightColor)
    }

    private val extractedSuggestions = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private val suggestionRow = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        addView(suggestionView, LinearLayout.LayoutParams(wrapContent, matchParent).apply {
            setMargins(0, dp(4), dp(4), dp(4))
        })
        addView(extractedSuggestions, LinearLayout.LayoutParams(wrapContent, matchParent))
    }

    override val root = HorizontalScrollView(ctx).apply {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        addView(suggestionRow, ViewGroup.LayoutParams(wrapContent, matchParent))
    }

    /** Extra paste targets are transient: they are never written to clipboard history. */
    fun setExtractedSuggestions(suggestions: List<ClipboardSuggestion>, onPaste: (String) -> Unit) {
        extractedSuggestions.removeAllViews()
        for (suggestion in suggestions) {
            val iconRes = when (suggestion.kind) {
                ClipboardSuggestionKind.Link -> R.drawable.ic_clipboard_link
                ClipboardSuggestionKind.Email -> R.drawable.ic_clipboard_email
                ClipboardSuggestionKind.Phone -> R.drawable.ic_clipboard_phone
                ClipboardSuggestionKind.Number -> R.drawable.ic_clipboard_number
            }
            val chip = TextView(ctx).apply {
                text = suggestion.text
                contentDescription = suggestion.text
                val iconDrawable = ctx.getDrawable(iconRes)?.mutate()?.apply {
                    setTint(theme.altKeyTextColor)
                    setBounds(0, 0, ctx.dp(20), ctx.dp(20))
                }
                setCompoundDrawablesRelative(iconDrawable, null, null, null)
                compoundDrawablePadding = ctx.dp(4)
                isSingleLine = true
                maxWidth = dp(160)
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(theme.altKeyTextColor)
                setPadding(dp(8), 0, dp(8), 0)
                background = rippleDrawable(theme.keyPressHighlightColor)
                setOnClickListener { onPaste(suggestion.text) }
            }
            extractedSuggestions.addView(
                chip, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, ctx.dp(4), ctx.dp(4), ctx.dp(4))
                }
            )
        }
        root.scrollTo(0, 0)
    }
}
