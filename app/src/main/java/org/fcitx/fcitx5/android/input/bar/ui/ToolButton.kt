/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewPropertyAnimator
import android.view.animation.Animation
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.borderlessRippleDrawable
import org.fcitx.fcitx5.android.utils.circlePressHighlightDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageResource
import splitties.views.padding

class ToolButton(context: Context) : CustomGestureView(context) {

    companion object {
        val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation
    }

    private val image = imageView {
        isClickable = false
        isFocusable = false
        padding = dp(10)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    var iconRotation: Float
        get() = image.rotation
        set(value) {
            image.rotation = value
        }

    constructor(context: Context, @DrawableRes icon: Int, theme: Theme) : this(context) {
        image.imageTintList = ColorStateList.valueOf(theme.altKeyTextColor)
        setIcon(icon)
        setPressHighlightColor(theme.keyPressHighlightColor)
        add(image, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun iconAnimate(): ViewPropertyAnimator = image.animate()

    fun setIcon(@DrawableRes icon: Int) {
        image.imageResource = icon
    }

    fun setIconTint(@ColorInt color: Int) {
        image.imageTintList = ColorStateList.valueOf(color)
    }

    private var recordingBackground: android.graphics.drawable.GradientDrawable? = null
    private var recordingPulse: ObjectAnimator? = null

    /**
     * Recording indicator: a filled accent-colored circle behind the icon with a breathing
     * animation, clearly visible even on light themes.
     */
    fun startRecordingIndicator(@ColorInt backgroundColor: Int, @ColorInt iconColor: Int) {
        setIconTint(iconColor)
        val circle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(backgroundColor)
        }
        recordingBackground = circle
        background = circle
        if (recordingPulse?.isRunning != true) {
            recordingPulse = ObjectAnimator.ofInt(circle, "alpha", 255, 140).apply {
                duration = 700L
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
            }.also { it.start() }
        }
    }

    fun stopRecordingIndicator(@ColorInt iconColor: Int, @ColorInt pressHighlightColor: Int) {
        recordingPulse?.cancel()
        recordingPulse = null
        recordingBackground = null
        image.alpha = 1f
        setIconTint(iconColor)
        setPressHighlightColor(pressHighlightColor)
    }

    fun setPressHighlightColor(@ColorInt color: Int) {
        background = if (disableAnimation) {
            circlePressHighlightDrawable(color)
        } else {
            borderlessRippleDrawable(color, dp(20))
        }
    }
}
