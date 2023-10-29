/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.styles.AndroidStyles
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable
import splitties.views.setPaddingDp

sealed class ClipboardInstructionUi(override val ctx: Context, protected val theme: Theme) : Ui {

    class Enable(ctx: Context, theme: Theme) : ClipboardInstructionUi(ctx, theme) {

        private val androidStyles = AndroidStyles(ctx)

        private val instructionText = textView {
            setText(R.string.instruction_enable_clipboard_listening)
            setPaddingDp(12, 8, 12, 8)
            setTextColor(theme.keyTextColor)
        }

        val enableButton = androidStyles.button.borderless {
            setText(R.string.clipboard_enable)
            setTextColor(theme.accentKeyBackgroundColor)
        }

        override val root = constraintLayout {
            add(instructionText, lParams(wrapContent, wrapContent) {
                topOfParent()
                startOfParent()
                endOfParent()
            })
            add(enableButton, lParams(wrapContent, wrapContent) {
                below(instructionText)
                endOfParent(dp(8))
            })
        }
    }

    class Empty(ctx: Context, theme: Theme) : ClipboardInstructionUi(ctx, theme) {

        private val icon = imageView {
            imageDrawable = drawable(R.drawable.ic_baseline_content_paste_24)!!.apply {
                setTint(theme.altKeyTextColor)
            }
        }

        private val instructionText = textView {
            setText(R.string.instruction_copy)
            setTextColor(theme.keyTextColor)
        }

        override val root = constraintLayout {
            add(icon, lParams(dp(90), dp(90)) {
                topOfParent(dp(24))
                startOfParent()
                endOfParent()
            })
            add(instructionText, lParams(wrapContent, wrapContent) {
                below(icon, dp(16))
                startOfParent()
                endOfParent()
            })
        }
    }
}
