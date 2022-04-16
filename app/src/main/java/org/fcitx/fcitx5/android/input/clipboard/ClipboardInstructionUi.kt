package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.PorterDuff
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.applyKeyTextColor
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import splitties.dimensions.dp
import splitties.resources.str
import splitties.views.dsl.appcompat.AppCompatStyles
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.imageResource
import splitties.views.verticalPadding

sealed class ClipboardInstructionUi(override val ctx: Context) : Ui {

    class Enable(ctx: Context) : ClipboardInstructionUi(ctx) {

        private val appCompatStyles = AppCompatStyles(ctx)

        private val instructionText = textView {
            text = str(R.string.instruction_enable_clipboard_listening)
            verticalPadding = dp(8)
            horizontalPadding = dp(12)
            ThemeManager.currentTheme.applyKeyTextColor(this)
        }

        val enableButton = appCompatStyles.button.borderless {
            text = str(R.string.clipboard_enable)
            setTextColor(ThemeManager.currentTheme.keyAccentBackgroundColor.resolve(context))
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

    class Empty(ctx: Context) : ClipboardInstructionUi(ctx) {

        private val icon = imageView {
            imageResource = R.drawable.ic_baseline_content_paste_24
            colorFilter =
                ThemeManager.currentTheme.funKeyColor.toColorFilter(PorterDuff.Mode.SRC_IN)
                    .resolve(context)
        }

        private val instructionText = textView {
            text = str(R.string.instruction_copy)
            ThemeManager.currentTheme.applyKeyTextColor(this)
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
