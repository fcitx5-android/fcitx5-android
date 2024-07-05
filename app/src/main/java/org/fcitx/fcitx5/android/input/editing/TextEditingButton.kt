/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.editing

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.borderedKeyBackgroundDrawable
import org.fcitx.fcitx5.android.input.keyboard.insetRadiusDrawable
import org.fcitx.fcitx5.android.utils.borderDrawable
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageResource
import kotlin.math.max

@SuppressLint("ViewConstructor")
class TextEditingButton(
    ctx: Context,
    private val theme: Theme,
    private val rippled: Boolean,
    private val bordered: Boolean,
    private val radius: Float,
    private val altStyle: Boolean = false
) : CustomGestureView(ctx) {

    // bordered
    private val shadowWidth = dp(1)
    private val hInset = dp(4)
    private val vInset = dp(4)

    // !bordered
    private val lineWidth = max(1, dp(1) / 2)

    init {
        if (bordered) {
            val bkgColor = if (altStyle) theme.altKeyBackgroundColor else theme.keyBackgroundColor
            background = borderedKeyBackgroundDrawable(
                bkgColor, theme.keyShadowColor,
                radius, shadowWidth, hInset, vInset
            )
            foreground = if (rippled) {
                RippleDrawable(
                    ColorStateList.valueOf(theme.keyPressHighlightColor), null,
                    insetRadiusDrawable(hInset, vInset, radius)
                )
            } else {
                StateListDrawable().apply {
                    addState(
                        intArrayOf(android.R.attr.state_pressed),
                        insetRadiusDrawable(hInset, vInset, radius, theme.keyPressHighlightColor)
                    )
                }
            }
        } else {
            background = borderDrawable(lineWidth, theme.dividerColor)
            foreground =
                if (rippled) rippleDrawable(theme.keyPressHighlightColor)
                else pressHighlightDrawable(theme.keyPressHighlightColor)
        }
    }

    val textView = textView {
        isClickable = false
        isFocusable = false
        background = null
        setTextColor(if (altStyle) theme.altKeyTextColor else theme.keyTextColor)
    }

    val imageView = imageView {
        isClickable = false
        isFocusable = false
        imageTintList = ColorStateList.valueOf(theme.altKeyTextColor)
    }

    fun setText(id: Int) {
        textView.setText(id)
        removeView(imageView)
        add(textView, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun setIcon(@DrawableRes icon: Int) {
        imageView.imageResource = icon
        removeView(textView)
        add(imageView, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun enableActivatedState() {
        textView.setTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_activated),
                    intArrayOf(android.R.attr.state_enabled)
                ),
                intArrayOf(
                    theme.genericActiveForegroundColor,
                    if (altStyle) theme.altKeyTextColor else theme.keyTextColor
                )
            )
        )
        imageView.imageTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_enabled)
            ),
            intArrayOf(
                theme.genericActiveForegroundColor,
                theme.altKeyTextColor
            )
        )
        background = if (bordered) {
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_activated),
                    borderedKeyBackgroundDrawable(
                        theme.genericActiveBackgroundColor, theme.keyShadowColor,
                        radius, shadowWidth, hInset, vInset
                    )
                )
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    borderedKeyBackgroundDrawable(
                        theme.keyBackgroundColor, theme.keyShadowColor,
                        radius, shadowWidth, hInset, vInset
                    )
                )
            }
        } else {
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_activated),
                    borderDrawable(
                        lineWidth,
                        theme.dividerColor,
                        theme.genericActiveBackgroundColor
                    )
                )
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    borderDrawable(lineWidth, theme.dividerColor)
                )
            }
        }
    }
}
