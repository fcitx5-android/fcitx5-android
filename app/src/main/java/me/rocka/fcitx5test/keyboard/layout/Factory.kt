package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.keyboard.layout.*
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.NO_THEME
import splitties.views.dsl.core.button
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.view
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
                            setOnClickListener { onAction(this, key.onPress(), false) }
                            if (key is LongPressButton) {
                                setOnLongClickListener {
                                    onAction(this, key.onLongPress(), true)
                                    true
                                }
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
                                when (row[index]) {
                                    is CapsButton -> {
                                        matchConstraintPercentWidth = 0.15F
                                    }
                                    is BackspaceButton -> {
                                        matchConstraintPercentWidth = 0.15F
                                    }
                                    is QuickPhraseButton -> {
                                        matchConstraintPercentWidth = 0.15F
                                    }
                                    is ReturnButton -> {
                                        matchConstraintPercentWidth = 0.15F
                                    }
                                    is SpaceButton -> {
                                        width = 0
                                    }
                                    else -> {
                                        matchConstraintPercentWidth = 0.1F
                                    }
                                }
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
            return CustomKeyboardView(root)
        }
    }

    private fun createButton(context: Context, btn: BaseButton): View {
        with(context) {
            return when (btn) {
                is CapsButton -> imageButton(R.id.button_caps) {
                    imageResource = R.drawable.ic_baseline_keyboard_capslock0_24
                }
                is BackspaceButton -> imageButton(R.id.button_backspace) {
                    imageResource = R.drawable.ic_baseline_backspace_24
                }
                is QuickPhraseButton -> imageButton(R.id.button_quickphrase) {
                    imageResource = R.drawable.ic_baseline_format_quote_24
                }
                is LangSwitchButton -> imageButton(R.id.button_lang) {
                    imageResource = R.drawable.ic_baseline_language_24
                }
                is SpaceButton -> button(R.id.button_space) {
                    isAllCaps = false
                }
                is ReturnButton -> imageButton(R.id.button_return) {
                    imageResource = R.drawable.ic_baseline_keyboard_return_24
                    backgroundTintList = styledColorSL(R.attr.colorAccent)
                    setColorFilter(styledColor(android.R.attr.colorForegroundInverse), SRC_IN)
                }
                is LongPressButton -> button {
                    padding = 0
                    text = "${btn.text}\n${btn.altText}"
                    textSize = dp(5.5f)
                }
                else -> button { text = btn.text }
            }.apply {
                setTheme(NO_THEME)
                elevation = dp(2f)
            }
        }
    }
}