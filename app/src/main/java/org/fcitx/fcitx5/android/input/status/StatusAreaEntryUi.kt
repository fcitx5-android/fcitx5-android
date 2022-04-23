package org.fcitx.fcitx5.android.input.status

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.ViewGroup
import android.widget.ImageView
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter

class StatusAreaEntryUi(override val ctx: Context, private val inputTheme: Theme) : Ui {

    private val bkgShape = ctx.dp(24f).let { r ->
        ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
    }

    val icon = imageView {
        background = bkgShape
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val label = textView {
        textSize = 12f
        gravity = gravityCenter
        setTextColor(inputTheme.keyTextColor)
    }

    override val root = constraintLayout {
        add(icon, lParams(dp(48), dp(48)) {
            topOfParent(dp(4))
            startOfParent()
            endOfParent()
            above(label)
        })
        add(label, lParams(wrapContent, wrapContent) {
            below(icon, dp(6))
            startOfParent()
            endOfParent()
        })
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96))
    }

    fun setEntry(entry: StatusAreaEntry) = with(ctx) {
        icon.setImageDrawable(drawable(entry.icon))
        icon.colorFilter = PorterDuffColorFilter(
            if (entry.active) inputTheme.accentKeyTextColor else inputTheme.keyTextColor,
            PorterDuff.Mode.SRC_IN
        )
        bkgShape.paint.color =
            if (entry.active) inputTheme.accentKeyBackgroundColor else inputTheme.keyBackgroundColor
        label.text = entry.label
    }
}
