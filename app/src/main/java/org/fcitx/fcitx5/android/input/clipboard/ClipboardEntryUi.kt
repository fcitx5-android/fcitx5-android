package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.TextUtils
import androidx.cardview.widget.CardView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.imageResource
import splitties.views.verticalPadding

class ClipboardEntryUi(override val ctx: Context, private val inputTheme: Theme) : Ui {

    val text = textView {
        maxLines = 4
        textSize = 14f
        verticalPadding = dp(4)
        horizontalPadding = dp(8)
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(inputTheme.keyTextColor)
    }

    val pin = imageView {
        imageResource = R.drawable.ic_baseline_push_pin_24
        colorFilter = PorterDuffColorFilter(inputTheme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        alpha = 0.3f
    }

    val wrapper = constraintLayout {
        add(text, lParams(matchParent, wrapContent) {
            topOfParent()
            bottomOfParent()
            startOfParent()
            endOfParent()
        })
        add(pin, lParams(dp(12), dp(12)) {
            bottomOfParent(dp(2))
            endOfParent(dp(2))
        })
    }

    override val root = view(::CardView) {
        minimumWidth = dp(40)
        minimumHeight = dp(30)
        isClickable = true
        foreground = rippleDrawable(inputTheme.keyPressHighlightColor)
        setCardBackgroundColor(inputTheme.clipboardEntryColor)
        cardElevation = 0f
        add(wrapper, lParams(matchParent, wrapContent))
    }
}