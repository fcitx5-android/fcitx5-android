package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.TextKeyboard
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable
import java.io.File

class KeyboardPreviewUi(override val ctx: Context, val theme: Theme) : Ui {

    var intrinsicWidth: Int = -1
        private set

    var intrinsicHeight: Int = -1
        private set

    private val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

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

    init {
        val (w, h) = keyboardWindowAspectRatio()
        keyboardWidth = w
        keyboardHeight = h
        setTheme(theme)
        intrinsicWidth = keyboardWidth
        // bar height
        intrinsicHeight = keyboardHeight + ctx.dp(40)
        root = frameLayout {
            add(fakeInputView, lParams(intrinsicWidth, intrinsicHeight))
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // bottom padding
                ViewCompat.getRootWindowInsets(v)?.let {
                    intrinsicHeight += it.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                }
                onSizeMeasured?.invoke(intrinsicWidth, intrinsicHeight)
                fakeInputView.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = intrinsicWidth
                    height = intrinsicHeight
                }
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }

    fun setBackground(drawable: Drawable) {
        bkg.imageDrawable = drawable
    }

    fun setTheme(theme: Theme) {
        setBackground(when (theme) {
            is Theme.Builtin -> ColorDrawable(
                if (ThemeManager.prefs.keyBorder.getValue()) theme.backgroundColor.color
                else theme.keyboardColor.color
            )
            is Theme.Custom -> theme.backgroundImage
                ?.croppedFilePath
                ?.takeIf { File(it).exists() }
                ?.let { BitmapDrawable(ctx.resources, BitmapFactory.decodeFile(it)) }
                ?: ColorDrawable(theme.backgroundColor.color)
        })
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
