package org.fcitx.fcitx5.android.input.status

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.ViewGroup
import android.widget.ImageView
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter

class StatusAreaEntryUi(override val ctx: Context) : Ui {

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
        setTextColor(styledColor(android.R.attr.colorForeground))
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
            styledColor(
                if (entry.active) android.R.attr.colorForegroundInverse
                else android.R.attr.colorControlNormal
            ),
            PorterDuff.Mode.SRC_IN
        )
        bkgShape.paint.color = styledColor(
            if (entry.active) android.R.attr.colorAccent
            else android.R.attr.colorButtonNormal
        )
        label.text = entry.label
    }
}
