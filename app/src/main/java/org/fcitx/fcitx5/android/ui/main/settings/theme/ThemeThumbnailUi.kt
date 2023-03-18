package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable
import splitties.views.setPaddingDp

class ThemeThumbnailUi(override val ctx: Context) : Ui {
    val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val bar = view(::View)

    val spaceBar = view(::View)

    val returnKey = view(::View)

    val checkMark = imageView {
        scaleType = ImageView.ScaleType.FIT_CENTER
        imageDrawable = ctx.drawable(R.drawable.ic_baseline_check_24)
    }

    val editButton = imageView {
        setPaddingDp(16, 4, 4, 16)
        scaleType = ImageView.ScaleType.FIT_CENTER
        imageDrawable = ctx.drawable(R.drawable.ic_baseline_edit_24)
    }

    override val root = constraintLayout {
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(2f)
        add(bkg, lParams(matchParent, matchParent))
        add(bar, lParams(matchParent, dp(14)))
        add(spaceBar, lParams(height = dp(10)) {
            centerHorizontally()
            bottomOfParent(dp(6))
            matchConstraintPercentWidth = 0.5f
        })
        add(returnKey, lParams(dp(14), dp(14)) {
            rightOfParent(dp(4))
            bottomOfParent(dp(4))
        })
        add(checkMark, lParams(dp(60), dp(60)) {
            centerInParent()
        })
        add(editButton, lParams(dp(44), dp(44)) {
            topOfParent()
            endOfParent()
        })
    }

    fun setTheme(theme: Theme, checked: Boolean = false) {
        root.apply {
            foreground = rippleDrawable(theme.keyPressHighlightColor)
        }
        bkg.imageDrawable = theme.backgroundDrawable()
        bar.backgroundColor = theme.barColor
        spaceBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(2f)
            setColor(theme.spaceBarColor)
        }
        returnKey.background = ShapeDrawable(OvalShape()).apply {
            paint.color = theme.accentKeyBackgroundColor
        }
        val foreground = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        editButton.apply {
            visibility = if (theme is Theme.Custom) View.VISIBLE else View.GONE
            colorFilter = foreground
            background = rippleDrawable(theme.keyPressHighlightColor)
        }
        setChecked(checked)
        checkMark.colorFilter = foreground
    }

    fun setChecked(checked: Boolean) {
        checkMark.visibility = if (checked) View.VISIBLE else View.GONE
    }
}