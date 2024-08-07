/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.annotation.ColorInt

fun rippleDrawable(
    @ColorInt color: Int,
    mask: Drawable = ColorDrawable(Color.WHITE)
): Drawable = RippleDrawable(ColorStateList.valueOf(color), null, mask)

fun borderlessRippleDrawable(
    @ColorInt color: Int,
    r: Int = RippleDrawable.RADIUS_AUTO
): Drawable = RippleDrawable(ColorStateList.valueOf(color), null, null).apply {
    radius = r
}

fun pressHighlightDrawable(
    @ColorInt color: Int
): Drawable = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
}

fun circlePressHighlightDrawable(
    @ColorInt color: Int
): Drawable = StateListDrawable().apply {
    addState(
        intArrayOf(android.R.attr.state_pressed),
        ShapeDrawable(OvalShape()).apply { paint.color = color }
    )
}

fun borderDrawable(
    width: Int,
    @ColorInt stroke: Int,
    @ColorInt background: Int = Color.TRANSPARENT
): Drawable = GradientDrawable().apply {
    setStroke(width, stroke)
    setColor(background)
}
