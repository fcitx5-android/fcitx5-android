package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.keyboard.layout.*
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource
import splitties.views.padding

object Factory {

    fun create(
        context: Context,
        keyLayout: List<List<BaseButton>>,
        onAction: (View, ButtonAction<*>, Boolean) -> Unit
    ): CustomKeyboardView {
        with(context) {
            val root = constraintLayout(R.id.keyboard_26) Root@{
                setTheme(R.style.Theme_AppCompat_DayNight)
                backgroundColor = styledColor(android.R.attr.colorBackground)
                val keyRows = keyLayout.map { row ->
                    val keyButtons = row.map { key ->
                        createButton(context, key).apply {
                            if (key is IPressKey) setOnClickListener {
                                onAction(this, key.onPress(), false)
                            }
                            if (key is ILongPressKey) setOnLongClickListener {
                                onAction(this, key.onLongPress(), true)
                                true
                            }
                        }
                    }
                    constraintLayout Row@{
                        keyButtons.forEachIndexed { index, button ->
                            addView(button, lParams {
                                topOfParent()
                                bottomOfParent()
                                if (index == 0) {
                                    startOfParent()
                                    horizontalChainStyle =
                                        ConstraintLayout.LayoutParams.CHAIN_PACKED
                                } else after(keyButtons[index - 1])
                                if (index == keyButtons.size - 1) endOfParent()
                                else before(keyButtons[index + 1])
                                val buttonDef = row[index]
                                matchConstraintPercentWidth = buttonDef.percentWidth
                            })
                        }
                    }
                }
                val candidateList = view(::RecyclerView, R.id.candidate_list) {}
                addView(candidateList, lParams {
                    height = dp(40)
                    topOfParent()
                    above(keyRows[0])
                    startOfParent()
                    endOfParent()
                })
                keyRows.forEachIndexed { index, row ->
                    addView(row, lParams {
                        height = dp(60)
                        if (index == 0) below(candidateList)
                        else below(keyRows[index - 1])
                        if (index == keyRows.size - 1) bottomOfParent()
                        else above(keyRows[index + 1])
                        startOfParent()
                        endOfParent()
                    })
                }
            }
            val wrapper = view(::FrameLayout) {
                add(root, lParams(matchParent, matchParent))
            }
            return CustomKeyboardView(wrapper)
        }
    }

    private fun createButton(context: Context, btn: BaseButton): View = with(context) {
        val view = when (btn) {
            is IImageKey -> imageButton {
                imageResource = btn.src
                if (btn is ITintKey) {
                    backgroundTintList = styledColorSL(btn.background)
                    colorFilter =
                        PorterDuffColorFilter(styledColor(btn.foreground), PorterDuff.Mode.SRC_IN)
                }
            }
            is ITextKey -> button {
                text = btn.displayText
                isAllCaps = false
            }
            else -> button {}
        }
        when (btn) {
            is IKeyId -> {
                view.id = btn.id
            }
        }
        view.apply {
            setTheme(NO_THEME)
            padding = 0
            elevation = dp(2f)
        }
        return view
    }
}