package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.*
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.view

@SuppressLint("ViewConstructor")
class PickerLayout(context: Context, theme: Theme) : ConstraintLayout(context) {

    class Keyboard(context: Context, theme: Theme) : BaseKeyboard(context, theme, Layout) {

        class SymbolKey(
            val symbol: String,
            percentWidth: Float = 0.1f,
            variant: Appearance.Variant = Appearance.Variant.Alternative,
        ) : KeyDef(
            Appearance.Text(
                displayText = symbol,
                textSize = 23f,
                typeface = Typeface.NORMAL,
                percentWidth = percentWidth,
                variant = variant
            ),
            setOf(
                Behavior.Press(action = KeyAction.CommitAction(symbol))
            )
        )

        companion object {
            val Layout: List<List<KeyDef>> = listOf(
                listOf(
                    LayoutSwitchKey("ABC", TextKeyboard.Name),
                    SymbolKey(","),
                    ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name),
                    SpaceKey(),
                    SymbolKey("."),
                    ReturnKey()
                )
            )
        }

        val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }
    }

    val embeddedKeyboard = Keyboard(context, theme)

    val pager = view(::ViewPager2) { }

    val tabsUi = PickerTabsUi(context, theme)

    val paginationUi = PickerPaginationUi(context, theme)

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
        add(paginationUi.root, lParams(matchConstraints, dp(2)) {
            centerHorizontally()
            below(pager, dp(-1))
        })
    }
}