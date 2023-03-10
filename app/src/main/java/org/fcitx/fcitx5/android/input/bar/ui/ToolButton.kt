package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.borderlessRippleDrawable
import org.fcitx.fcitx5.android.utils.circlePressHighlightDrawable
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageDrawable
import splitties.views.padding

class ToolButton(context: Context) : CustomGestureView(context) {

    companion object {
        val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation
    }

    constructor(context: Context, @DrawableRes icon: Int, theme: Theme) : this(context) {
        setIcon(icon, theme.altKeyTextColor)
        setPressHighlightColor(theme.keyPressHighlightColor)
    }

    val image = imageView {
        isClickable = false
        isFocusable = false
        padding = dp(10)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    init {
        add(image, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun setIcon(@DrawableRes icon: Int, @ColorInt color: Int) {
        image.imageDrawable = drawable(icon)
        image.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun setPressHighlightColor(@ColorInt color: Int) {
        background = if (disableAnimation) {
            circlePressHighlightDrawable(color)
        } else {
            borderlessRippleDrawable(color, dp(20))
        }
    }
}
