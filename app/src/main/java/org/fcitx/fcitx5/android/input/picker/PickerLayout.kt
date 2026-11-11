/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.allViews
import org.fcitx.fcitx5.android.input.popup.PopupAction
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.*
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.view
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class PickerLayout(context: Context, theme: Theme, switchKey: KeyDef) :
    ConstraintLayout(context) {

    class Keyboard(context: Context, theme: Theme, switchKey: KeyDef) : BaseKeyboard(
        context, theme, listOf(
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                PunctuationKey(","),
                switchKey,
                SpaceKey(),
                PunctuationKey("."),
                ReturnKey()
            )
        )
    ) {

        class PunctuationKey(val symbol: String) : KeyDef(
            Appearance.Text(
                displayText = symbol,
                textSize = 23f,
                percentWidth = 0.1f,
                variant = Appearance.Variant.Alternative
            ),
            setOf(
                Behavior.Press(KeyAction.FcitxKeyAction(symbol))
            )
        )

        private var punctuationPolicy: PickerPolicy = DefaultPickerPolicy()

        fun updatePunctuationLabels(policy: PickerPolicy) {
            punctuationPolicy = policy
            allViews.filterIsInstance<TextKeyView>().forEach { key ->
                val label = (key.def as KeyDef.Appearance.Text).displayText
                if (label != "," && label != ".") return@forEach
                val transformed = policy.transform(label)
                key.mainText.text = transformed
                val popup = if (transformed != label) policy.popup(label) else null
                key.setOnLongClickListener(if (popup == null) null else android.view.View.OnLongClickListener {
                    key.updateBounds()
                    onPopupAction(PopupAction.ShowKeyboardAction(key.id, popup, key.bounds))
                    false
                })
                key.swipeEnabled = popup != null
                key.onGestureListener = if (popup == null) null else CustomGestureView.OnGestureListener { _, event ->
                    when (event.type) {
                        CustomGestureView.GestureType.Move -> onPopupChangeFocus(key.id, event.x, event.y)
                        CustomGestureView.GestureType.Up -> onPopupTrigger(key.id)
                        else -> false
                    }
                }
            }
        }

        override fun onAction(action: KeyAction, source: KeyActionListener.Source) {
            val transformed = if (action is KeyAction.FcitxKeyAction) {
                action.copy(act = punctuationPolicy.transform(action.act))
            } else action
            super.onAction(transformed, source)
        }

        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

        override fun onReturnDrawableUpdate(returnDrawable: Int) {
            `return`.img.imageResource = returnDrawable
        }
    }

    val embeddedKeyboard = Keyboard(context, theme, switchKey)

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