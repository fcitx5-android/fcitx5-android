package org.fcitx.fcitx5.android.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import androidx.annotation.ColorInt

fun View.rippleDrawable(@ColorInt color: Int) =
    RippleDrawable(ColorStateList.valueOf(color), null, ColorDrawable(Color.WHITE))

fun View.borderlessRippleDrawable(@ColorInt color: Int, r: Int = RippleDrawable.RADIUS_AUTO) =
    RippleDrawable(ColorStateList.valueOf(color), null, null).apply {
        radius = r
    }

fun View.pressHighlightDrawable(@ColorInt color: Int) = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
}

fun View.circlePressHighlightDrawable(@ColorInt color: Int) = StateListDrawable().apply {
    addState(
        intArrayOf(android.R.attr.state_pressed),
        ShapeDrawable(OvalShape()).apply { paint.color = color }
    )
}

fun View.borderDrawable(
    width: Int,
    @ColorInt stroke: Int,
    @ColorInt background: Int = Color.TRANSPARENT
) = GradientDrawable().apply {
    setStroke(width, stroke)
    setColor(background)
}
