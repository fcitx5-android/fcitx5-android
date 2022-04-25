package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.TextKeyboard
import org.fcitx.fcitx5.android.utils.keyboardWindowAspectRatio
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.bottomPadding
import splitties.views.dsl.core.*
import timber.log.Timber

class KeyboardPreviewUi(override val ctx: Context, val theme: Theme) : Ui {

    var intrinsicWidth: Int = -1
        private set

    var intrinsicHeight: Int = -1
        private set

    private val fakeKawaiiBar = view(::View)

    private var keyboardWidth = -1
    private var keyboardHeight = -1
    private lateinit var fakeKeyboardWindow: TextKeyboard

    override val root = verticalLayout {
        add(fakeKawaiiBar, lParams(matchParent, dp(40)))
    }

    init {
        val (x, y) = ctx.keyboardWindowAspectRatio()
        keyboardWidth = x
        keyboardHeight = y
        setTheme(theme)
        intrinsicWidth = keyboardWidth
        // bar height
        intrinsicHeight = keyboardHeight + ctx.dp(40)
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // bottom padding
                ViewCompat.getRootWindowInsets(v)?.let {
                    val bottom = it.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    Timber.d("Bottom padding: $bottom")
                    intrinsicHeight += bottom
                    root.bottomPadding = bottom
                }
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }

    fun setBackground(drawable: Drawable) {
        root.background = drawable
    }

    fun setTheme(theme: Theme) {
        if (this::fakeKeyboardWindow.isInitialized) {
            root.removeView(fakeKeyboardWindow)
        }
        fakeKawaiiBar.backgroundColor = theme.barColor
        fakeKeyboardWindow = TextKeyboard(ctx, theme)
        root.apply { add(fakeKeyboardWindow, lParams(keyboardWidth, keyboardHeight)) }
        when (theme) {
            is Theme.Builtin -> {
                root.backgroundColor = theme.backgroundColor
            }
            is Theme.CustomBackground -> {
                root.background = Drawable.createFromStream(theme.backgroundImage.inputStream(), "")
            }
        }
    }
}
