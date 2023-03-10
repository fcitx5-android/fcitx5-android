package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.TextUtils
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource

class ClipboardSuggestionUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val icon = imageView {
        imageResource = R.drawable.ic_clipboard
        colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
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

    override val root = constraintLayout {
        add(suggestionView, lParams(wrapContent, matchConstraints) {
            centerInParent()
            verticalMargin = dp(4)
        })
    }
}
