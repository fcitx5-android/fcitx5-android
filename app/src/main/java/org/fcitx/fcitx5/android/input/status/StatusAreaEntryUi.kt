package org.fcitx.fcitx5.android.input.status

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.ViewGroup
import android.widget.ImageView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.applyKeyTextColor
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter

class StatusAreaEntryUi(override val ctx: Context, private val intputTheme: Theme) : Ui {

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
        intputTheme.applyKeyTextColor(this)
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
        icon.colorFilter = (if (entry.active)
            intputTheme.keyTextColorInverse
        else
            intputTheme.funKeyColor).toColorFilter(PorterDuff.Mode.SRC_IN)
            .resolve(ctx)
        bkgShape.paint.color = (
                if (entry.active) intputTheme.keyAccentBackgroundColor
                else intputTheme.keyBackgroundColorBordered).resolve(ctx)
        label.text = entry.label
    }
}
