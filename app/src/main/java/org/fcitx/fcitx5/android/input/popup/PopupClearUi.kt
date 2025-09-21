/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams

/**
 * A dedicated popup container for "clear all" confirmation shown above Backspace.
 *
 * This replicates the former preview visual and placement (previously PreviewStyle.FitAbove):
 * - widthScale = 1.2x key width
 * - heightScale = 0.7x key height
 * - placed above the key with a small gap
 *
 * Danger hint follows the old visual (previously named "armed"): red background with white icon.
 */
class PopupClearUi(
    override val ctx: Context,
    theme: Theme,
    outerBounds: Rect,
    triggerBounds: Rect,
    private val initialDanger: Boolean,
    onDismissSelf: PopupContainerUi.() -> Unit = {},
) : PopupContainerUi(ctx, theme, outerBounds, triggerBounds, onDismissSelf), Ui {

    private val gap = ctx.dp(4)

    // Compute popup size using the same scales as previous FitAbove preview
    private val keyW = triggerBounds.width()
    private val keyH = triggerBounds.height()
    private val widthScale = 1.2f
    private val heightScale = 0.7f
    private val w = (keyW * widthScale).toInt()
    private val h = (keyH * heightScale).toInt()

    private val normalBackground = GradientDrawable().apply {
        cornerRadius = ctx.dp(8).toFloat()
        setColor(theme.popupBackgroundColor)
    }

    private val dangerBackground = GradientDrawable().apply {
        cornerRadius = ctx.dp(8).toFloat()
        // Preserve the previous danger hint color effect (red-ish composite)
        val active = ColorUtils.compositeColors(theme.keyPressHighlightColor, 0xFFE53935.toInt())
        setColor(active)
    }

    private val iconView: ImageView

    private var danger: Boolean = initialDanger
        set(value) {
            field = value
            root.background = if (value) dangerBackground else normalBackground
            val tint = if (value) 0xFFFFFFFF.toInt() else theme.popupTextColor
            iconView.setColorFilter(tint)
        }

    override val root: View = frameLayout {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(2f)
        background = normalBackground
        iconView = imageView {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_baseline_delete_sweep_24)
        }
        add(iconView, lParams(w, h))
    }

    init {
        danger = initialDanger
    }

    fun setDangerHint(value: Boolean) {
        danger = value
    }

    /**
     * Center horizontally over the trigger key, with clamping inside outer bounds.
     */
    override val offsetX: Int
        get() {
            val centerX = (triggerBounds.left + triggerBounds.right) / 2
            val desiredLeft = centerX - w / 2
            val minLeft = outerBounds.left
            val maxLeft = outerBounds.right - w
            val clamped = desiredLeft.coerceIn(minLeft, maxLeft)
            return clamped - triggerBounds.left
        }

    /**
     * Place fully above the key with a small gap; clamp to top if necessary.
     */
    override val offsetY: Int
        get() {
            val desiredTop = triggerBounds.top - gap - h
            val minTop = outerBounds.top
            val clamped = desiredTop.coerceAtLeast(minTop)
            return clamped - triggerBounds.top
        }

    override fun onChangeFocus(x: Float, y: Float): Boolean {
        // x/y are relative to the container; mark highlighted if inside, else clear.
        // Do NOT auto-dismiss while finger is down: spec expects sliding back
        // to the key to turn white (stay visible) instead of disappearing.
        val inside = x >= 0 && y >= 0 && x < w && y < h
        danger = inside
        return false
    }

    override fun onTrigger(): KeyAction? {
        return if (danger) {
            // Extra safety: ensure we dismiss ourselves once triggered
            onDismissSelf(this)
            KeyAction.ClearAllAction
        } else null
    }
}
