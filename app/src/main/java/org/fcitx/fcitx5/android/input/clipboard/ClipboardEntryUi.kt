package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.PorterDuff
import android.text.TextUtils
import androidx.cardview.widget.CardView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.applyKeyTextColor
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.imageResource
import splitties.views.verticalPadding

class ClipboardEntryUi(override val ctx: Context, private val intputTheme: Theme) : Ui {

    val text = textView {
        maxLines = 4
        textSize = 14f
        verticalPadding = dp(4)
        horizontalPadding = dp(8)
        ellipsize = TextUtils.TruncateAt.END
        intputTheme.applyKeyTextColor(this)
    }

    val pin = imageView {
        imageResource = R.drawable.ic_baseline_push_pin_24
        colorFilter =
            intputTheme.keyTextColorInverse.toColorFilter(PorterDuff.Mode.SRC_IN)
                .resolve(context)
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
        foreground = styledDrawable(android.R.attr.selectableItemBackground)
        setCardBackgroundColor(intputTheme.clipboardEntryColor.resolve(context))
        cardElevation = 0f
        add(wrapper, lParams(matchParent, wrapContent))
    }
}