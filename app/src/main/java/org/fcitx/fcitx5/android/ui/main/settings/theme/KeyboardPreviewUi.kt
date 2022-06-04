package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
            centerVertically()
            centerHorizontally()
        })
        add(fakeKawaiiBar, lParams(height = dp(40)) {
            centerHorizontally()
        })
    }

    override val root: FrameLayout

    var onSizeMeasured: ((Int, Int) -> Unit)? = null

    private fun keyboardWindowAspectRatio(): Pair<Int, Int> {
        val w = ctx.resources.displayMetrics.widthPixels
        val h = ctx.resources.displayMetrics.heightPixels
        val ratio = AppPrefs.getInstance().keyboard.run {
            when (ctx.resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> this.keyboardHeightPercentLandscape
                else -> this.keyboardHeightPercent
            }.getValue()
        }
        return w to (h * ratio / 100)
    }

    /**
     * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r48/services/core/java/com/android/server/wm/DisplayPolicy.java#3221
     * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r48/services/core/java/com/android/server/wm/DisplayPolicy.java#3059
     */
    private fun navbarHeight() = ctx.resources.run {
        val id = getIdentifier("navigation_bar_frame_height", "dimen", "android")
        if (id > 0) getDimensionPixelSize(id) else 0
    }

    init {
        val (w, h) = keyboardWindowAspectRatio()
        keyboardWidth = w
        keyboardHeight = h
        setTheme(theme)
        root = object : FrameLayout(ctx) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                recalculateSize()
                onSizeMeasured?.invoke(intrinsicWidth, intrinsicHeight)
            }

            override fun onConfigurationChanged(newConfig: Configuration?) {
                recalculateSize()
            }
        }.apply {
            add(fakeInputView, lParams())
        }
    }

    fun recalculateSize() {
        val (w, h) = keyboardWindowAspectRatio()
        keyboardWidth = w
        keyboardHeight = h
        fakeKeyboardWindow.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = keyboardWidth
            height = keyboardHeight
        }
        intrinsicWidth = keyboardWidth
        // bar height
        intrinsicHeight = barHeight + keyboardHeight
        // bottom padding
        if (ThemeManager.prefs.navbarBackground.getValue() == NavbarBackground.Full) {
            ViewCompat.getRootWindowInsets(root)?.also {
                // IME window has different navbar height when system navigation in "gesture navigation" mode
                // thus the inset from Activity root window is unreliable
                if (it.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom > 0 ||
                    // in case navigation hint was hidden ...
                    it.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom > 0
                ) {
                    intrinsicHeight = barHeight + keyboardHeight + navbarHeight()
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
        fakeKawaiiBar.backgroundColor =
            if (ThemeManager.prefs.keyBorder.getValue()) Color.TRANSPARENT
            else theme.barColor.color
        fakeKeyboardWindow = TextKeyboard(ctx, theme)
        fakeInputView.apply {
            add(fakeKeyboardWindow, lParams(keyboardWidth, keyboardHeight) {
                below(fakeKawaiiBar)
                centerHorizontally()
            })
        }
    }
}
