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
import splitties.views.imageResource
import splitties.views.setPaddingDp

class ClipboardEntryUi(override val ctx: Context, private val theme: Theme) : Ui {

    val text = textView {
        maxLines = 4
        textSize = 14f
        setPaddingDp(8, 4, 8, 4)
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(theme.keyTextColor)
    }

    val pin = imageView {
        imageResource = R.drawable.ic_baseline_push_pin_24
        colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
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
        foreground = rippleDrawable(theme.keyPressHighlightColor)
        setCardBackgroundColor(theme.clipboardEntryColor)
        cardElevation = 0f
        add(wrapper, lParams(matchParent, wrapContent))
    }
}