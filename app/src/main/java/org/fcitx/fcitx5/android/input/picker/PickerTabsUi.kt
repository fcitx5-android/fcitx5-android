package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.imageDrawable

class PickerTabsUi(override val ctx: Context, val theme: Theme) : Ui {

    companion object {
        val keyRipple by ThemeManager.prefs.keyRippleEffect
    }

    inner class TabUi : Ui {
        override val ctx: Context
            get() = this@PickerTabsUi.ctx

        var position: Int = -1

        val label = textView {
            textSize = 14f // sp
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.keyTextColor.color)
        }

        val icon = imageView {
            colorFilter = PorterDuffColorFilter(theme.keyTextColor.color, PorterDuff.Mode.SRC_IN)
        }

        override val root = view(::CustomGestureView) {
            add(label, lParams {
                gravity = gravityCenter
            })
            add(icon, lParams {
                gravity = gravityCenter
            })
            if (keyRipple) {
                background = rippleDrawable(theme.keyPressHighlightColor.color)
            } else {
                foreground = pressHighlightDrawable(theme.keyPressHighlightColor.color)
            }
            setOnClickListener {
                onTabClick(this@TabUi)
            }
        }

        fun setLabel(str: String) {
            label.text = str
            icon.isVisible = false
            label.isVisible = true
        }

        fun setIcon(@DrawableRes src: Int) {
            icon.imageDrawable = ctx.drawable(src)
            label.isVisible = false
            icon.isVisible = true
        }

        fun setActive(active: Boolean) {
            var color = theme.keyTextColor.color
            if (!active) color = ColorUtils.setAlphaComponent(color, 0x4c)
            label.setTextColor(color)
            icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private var tabs: Array<TabUi> = arrayOf()
    private var selected = -1

    private var onTabClick: (TabUi.(Int) -> Unit)? = null

    override val root = constraintLayout { }

    fun setTabs(labels: List<String>) {
        tabs.forEach { root.removeView(it.root) }
        selected = -1
        tabs = Array(labels.size) {
            TabUi().apply {
                position = it
                setLabel(labels[it])
                setActive(false)
            }
        }
        tabs.forEachIndexed { i, tabUi ->
            root.add(tabUi.root, root.lParams {
                centerVertically()
                if (i == 0) startOfParent() else after(tabs[i - 1].root)
                if (i == tabs.size - 1) endOfParent() else before(tabs[i + 1].root)
            })
        }
    }

    fun activateTab(index: Int) {
        if (index == selected) return
        if (selected >= 0) {
            tabs[selected].setActive(false)
        }
        tabs[index].setActive(true)
        selected = index
    }

    private fun onTabClick(tabUi: TabUi) {
        onTabClick?.invoke(tabUi, tabUi.position)
    }

    fun setOnTabClickListener(listener: (TabUi.(Int) -> Unit)? = null) {
        onTabClick = listener
    }
}
