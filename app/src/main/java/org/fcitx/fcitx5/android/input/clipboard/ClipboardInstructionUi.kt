package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.appcompat.AppCompatStyles
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.imageResource
import splitties.views.verticalPadding

sealed class ClipboardInstructionUi(override val ctx: Context, protected val theme: Theme) : Ui {

    class Enable(ctx: Context, theme: Theme) : ClipboardInstructionUi(ctx, theme) {

        private val appCompatStyles = AppCompatStyles(ctx)

        private val instructionText = textView {
            setText(R.string.instruction_enable_clipboard_listening)
            verticalPadding = dp(8)
            horizontalPadding = dp(12)
            setTextColor(theme.keyTextColor.color)
        }

        val enableButton = appCompatStyles.button.borderless {
            setText(R.string.clipboard_enable)
            setTextColor(theme.accentKeyBackgroundColor.color)
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
            imageResource = R.drawable.ic_baseline_content_paste_24
            colorFilter = PorterDuffColorFilter(theme.altKeyTextColor.color, PorterDuff.Mode.SRC_IN)
        }

        private val instructionText = textView {
            setText(R.string.instruction_copy)
            setTextColor(theme.keyTextColor.color)
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
