/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeManager.Prefs.NavbarBackground
import org.fcitx.fcitx5.android.input.keyboard.TextKeyboard
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

class KeyboardPreviewUi(override val ctx: Context, val theme: Theme) : Ui {

    var intrinsicWidth: Int = -1
        private set

    var intrinsicHeight: Int = -1
        private set

    private val keyboardPrefs = AppPrefs.getInstance().keyboard
    private val keyboardHeightPercent by keyboardPrefs.keyboardHeightPercent
    private val keyboardHeightPercentLandscape by keyboardPrefs.keyboardHeightPercentLandscape
    private val keyboardSidePadding by keyboardPrefs.keyboardSidePadding
    private val keyboardSidePaddingLandscape by keyboardPrefs.keyboardSidePaddingLandscape
    private val keyboardBottomPadding by keyboardPrefs.keyboardBottomPadding
    private val keyboardBottomPaddingLandscape by keyboardPrefs.keyboardBottomPaddingLandscape

    private val keyboardSidePaddingPx: Int
        get() {
            val value = when (ctx.resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardSidePaddingLandscape
                else -> keyboardSidePadding
            }
            return ctx.dp(value)
        }

    private val keyboardBottomPaddingPx: Int
        get() {
            val value = when (ctx.resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardBottomPaddingLandscape
                else -> keyboardBottomPadding
            }
            return ctx.dp(value)
        }

    private val navbarBackground by ThemeManager.prefs.navbarBackground
    private val keyBorder by ThemeManager.prefs.keyBorder

    private val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val barHeight = ctx.dp(40)
    private val fakeKawaiiBar = view(::View)

    private var keyboardWidth = -1
    private var keyboardHeight = -1
    private lateinit var fakeKeyboardWindow: TextKeyboard

    private val fakeInputView = constraintLayout {
        add(bkg, lParams {
            centerInParent()
        })
        add(fakeKawaiiBar, lParams(height = dp(40)) {
            centerHorizontally()
        })
    }

    override val root = object : FrameLayout(ctx) {
        init {
            add(fakeInputView, lParams())
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            recalculateSize()
            onSizeMeasured?.invoke(intrinsicWidth, intrinsicHeight)
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            recalculateSize()
        }
    }

    var onSizeMeasured: ((Int, Int) -> Unit)? = null

    private fun keyboardWindowAspectRatio(): Pair<Int, Int> {
        val resources = ctx.resources
        val displayMetrics = resources.displayMetrics
        val w = displayMetrics.widthPixels
        val h = displayMetrics.heightPixels
        val hPercent = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> keyboardHeightPercentLandscape
            else -> keyboardHeightPercent
        }
        return w to (h * hPercent / 100)
    }

    /**
     * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r48/services/core/java/com/android/server/wm/DisplayPolicy.java#3221
     * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r48/services/core/java/com/android/server/wm/DisplayPolicy.java#3059
     */
    private fun navbarHeight() = ctx.resources.run {
        @SuppressLint("DiscouragedApi")
        val id = getIdentifier("navigation_bar_frame_height", "dimen", "android")
        if (id > 0) getDimensionPixelSize(id) else 0
    }

    init {
        val (w, h) = keyboardWindowAspectRatio()
        keyboardWidth = w
        keyboardHeight = h
        setTheme(theme)
    }

    fun recalculateSize() {
        val (w, h) = keyboardWindowAspectRatio()
        keyboardWidth = w
        keyboardHeight = h
        fakeKeyboardWindow.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = keyboardHeight
            horizontalMargin = keyboardSidePaddingPx
        }
        intrinsicWidth = keyboardWidth
        // KawaiiBar height + WindowManager view height
        intrinsicHeight = barHeight + keyboardHeight
        // extra bottom padding
        intrinsicHeight += keyboardBottomPaddingPx
        // windowInsets navbar padding
        if (navbarBackground == NavbarBackground.Full) {
            ViewCompat.getRootWindowInsets(root)?.also {
                // IME window has different navbar height when system navigation in "gesture navigation" mode
                // thus the inset from Activity root window is unreliable
                if (it.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom > 0 ||
                    // in case navigation hint was hidden ...
                    it.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom > 0
                ) {
                    intrinsicHeight += navbarHeight()
                }
            }
        }
        fakeInputView.updateLayoutParams<FrameLayout.LayoutParams> {
            width = intrinsicWidth
            height = intrinsicHeight
        }
    }

    fun setBackground(drawable: Drawable) {
        bkg.imageDrawable = drawable
    }

    fun setTheme(theme: Theme, background: Drawable? = null) {
        setBackground(background ?: theme.backgroundDrawable(keyBorder))
        if (this::fakeKeyboardWindow.isInitialized) {
            fakeInputView.removeView(fakeKeyboardWindow)
        }
        fakeKawaiiBar.backgroundColor = if (keyBorder) Color.TRANSPARENT else theme.barColor
        fakeKeyboardWindow = TextKeyboard(ctx, theme).also {
            it.onAttach()
        }
        fakeInputView.apply {
            add(fakeKeyboardWindow, lParams(matchConstraints, keyboardHeight) {
                below(fakeKawaiiBar)
                centerHorizontally(keyboardSidePaddingPx)
            })
        }
    }
}
