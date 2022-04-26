package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

class ThemeThumbnailUi(override val ctx: Context) : Ui {
    val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val bar = view(::View)

    val spaceBar = view(::View)

    val returnKey = view(::View)

    override val root = constraintLayout {
        foreground = styledDrawable(R.attr.selectableItemBackground)
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(2f)
        add(bkg, lParams(matchParent, matchParent))
        add(bar, lParams(matchParent, dp(14)))
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

    fun setTheme(theme: Theme, checked: Boolean = false) {
        bkg.imageDrawable = when (theme) {
            is Theme.Builtin -> ColorDrawable(theme.backgroundColor.color)
            is Theme.Custom -> theme.backgroundImage?.let {
                BitmapDrawable(ctx.resources, BitmapFactory.decodeFile(it.first))
            } ?: ColorDrawable(theme.backgroundColor.color)
        }
        bar.backgroundColor = theme.barColor.color
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
