package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.ImageView
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.imageDrawable

class ThemeThumbnailUi(override val ctx: Context) : Ui {
    val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val spaceBar = imageView()

    val returnKey = imageView {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    override val root = constraintLayout {
        add(bkg, lParams {
            topOfParent()
            startOfParent()
            endOfParent()
            bottomOfParent()
        })
        add(spaceBar, lParams(height = dp(10)) {
            startOfParent()
            endOfParent()
            bottomOfParent(dp(6))
            matchConstraintPercentWidth = 0.5f
        })
        add(returnKey, lParams(dp(14), dp(14)) {
            endOfParent(dp(4))
            bottomOfParent(dp(4))
        })
    }

    fun setTheme(theme: Theme) {
        bkg.imageDrawable = when (theme) {
            is Theme.Builtin -> ColorDrawable(theme.backgroundColor.color)
            is Theme.Custom -> bkg.imageDrawable
        }
        spaceBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(2f)
            setColor(theme.spaceBarColor.color)
        }
        returnKey.background = ShapeDrawable(OvalShape()).apply {
            paint.color = theme.accentKeyBackgroundColor.color
        }
    }
}
