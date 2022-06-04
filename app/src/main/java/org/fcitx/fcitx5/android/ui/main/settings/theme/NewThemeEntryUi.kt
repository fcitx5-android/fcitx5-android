package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

class NewThemeEntryUi(override val ctx: Context) : Ui {
    val text = textView {
        setText(R.string.new_theme)
        setTextColor(Color.WHITE)
    }

    val icon = imageView {
        imageDrawable = ctx.drawable(R.drawable.ic_baseline_plus_24)
        colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
    }

    override val root = constraintLayout {
        foreground = styledDrawable(androidx.appcompat.R.attr.selectableItemBackground)
        background = ctx.drawable(R.drawable.bkg_theme_choose_image)
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(2f)
        add(icon, lParams(dp(24), dp(24)) {
            topOfParent()
            centerHorizontally()
            above(text, dp(4))
            verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
        })
        add(text, lParams(wrapContent, wrapContent) {
            below(icon)
            centerHorizontally()
            bottomOfParent()
        })
    }
}
