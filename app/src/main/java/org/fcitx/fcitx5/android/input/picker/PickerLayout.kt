package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.*
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.view

@SuppressLint("ViewConstructor")
class PickerLayout(context: Context, theme: Theme) : ConstraintLayout(context) {

    companion object {
        private val rippleEffect by ThemeManager.prefs.keyRippleEffect
    }

    class Keyboard(context: Context, theme: Theme) : BaseKeyboard(context, theme, Layout) {
        companion object {
            val Layout: List<List<KeyDef>> = listOf(
                listOf(
                    LayoutSwitchKey("ABC", TextKeyboard.Name),
                    SymbolKey(",", 0.1f, KeyDef.Appearance.Variant.Alternative),
                    ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name),
                    SpaceKey(),
                    SymbolKey(".", 0.1f, KeyDef.Appearance.Variant.Alternative),
                    ReturnKey()
                )
            )
        }

        val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }
    }

    val embeddedKeyboard = Keyboard(context, theme)

    val pager = view(::ViewPager2) { }

    val tab = view(::TabLayout) {
        setSelectedTabIndicator(null)
        val foregroundColors = ColorStateList(
            arrayOf(
                SELECTED_STATE_SET,
                EMPTY_STATE_SET
            ),
            intArrayOf(
                theme.keyTextColor.color,
                ColorUtils.setAlphaComponent(theme.keyTextColor.color, 76)
            )
        )
        tabIconTint = foregroundColors
        tabTextColors = foregroundColors
        // TODO: show tab press highlight when ripple effect not enabled
        tabRippleColor = if (rippleEffect) ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected, android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_selected)
            ),
            intArrayOf(
                theme.keyPressHighlightColor.color,
                Color.TRANSPARENT
            )
        ) else null
    }

    init {
        add(pager, lParams {
            topOfParent()
            centerHorizontally()
            above(embeddedKeyboard)
        })
        add(embeddedKeyboard, lParams {
            below(pager)
            centerHorizontally()
            bottomOfParent()
            matchConstraintPercentHeight = 0.25f
        })
    }
}