/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.view.ViewGroup
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.keyboard.ImageKeyView
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.FcitxKeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SymAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener.Source
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.keyboard.KeyView
import org.fcitx.fcitx5.android.input.keyboard.TextKeyView
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.bottomToTopOf
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.leftToRightOf
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.rightToLeftOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.constraintlayout.topToBottomOf
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent

class PickerPageUi(override val ctx: Context, val theme: Theme, private val density: Density) : Ui {

    enum class Density(
        val pageSize: Int,
        val columnCount: Int,
        val rowCount: Int,
        val textSize: Float,
        val showBackspace: Boolean
    ) {
        // symbol: 10/10/8, backspace on bottom right
        High(28, 10, 3, 19f, true),

        // emoji: 7/7/6, backspace on bottom right
        Medium(20, 7, 3, 23.7f, true),

        // emoticon: 4/4/4, no backspace
        Low(12, 4, 3, 19f, false)
    }

    companion object {
        val BackspaceAppearance = Appearance.Image(
            src = R.drawable.ic_baseline_backspace_24,
            variant = Variant.Alternative,
            border = Border.Off,
            viewId = R.id.button_backspace
        )

        val BackspaceAction = SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace))

        private var popupOnKeyPress by AppPrefs.getInstance().keyboard.popupOnKeyPress
    }

    var keyActionListener: KeyActionListener? = null
    var popupActionListener: PopupActionListener? = null

    private val keyAppearance = Appearance.Text(
        displayText = "",
        textSize = density.textSize,
        variant = Variant.Normal,
        border = Border.Off
    )

    private val keyViews = Array(density.pageSize) {
        TextKeyView(ctx, theme, keyAppearance).apply {
            if (density == Density.Low) {
                mainText.apply {
                    scaleMode = AutoScaleTextView.Mode.Proportional
                    setPadding(hMargin, vMargin, hMargin, vMargin)
                }
            }
        }
    }

    private val backspaceKey = ImageKeyView(ctx, theme, BackspaceAppearance).apply {
        setOnClickListener { onBackspaceClick() }
        repeatEnabled = true
        onRepeatListener = { onBackspaceClick() }
    }

    private fun onBackspaceClick() {
        keyActionListener?.onKeyAction(BackspaceAction, Source.Keyboard)
    }

    override val root = constraintLayout {
        val columnCount = density.columnCount
        val rowCount = density.rowCount
        val keyWidth = 1f / columnCount
        when (density) {
            Density.High -> {
                keyViews.forEachIndexed { i, keyView ->
                    val row = i / columnCount
                    val column = i % columnCount
                    add(keyView, lParams {
                        // layout_constraintTop_to
                        if (row == 0) {
                            // first row, align top to top of parent
                            topOfParent()
                        } else {
                            // not first row, align top to bottom of first view in last row
                            topToBottomOf(keyViews[(row - 1) * columnCount])
                        }
                        // layout_constraintBottom_to
                        if (row == rowCount - 1) {
                            // last row, align bottom to bottom of parent
                            bottomOfParent()
                        } else {
                            // not last row, align bottom to top of first view in next row
                            bottomToTopOf(keyViews[(row + 1) * columnCount])
                        }
                        // layout_constraintRight_to
                        if (i == keyViews.size - 1) {
                            // last key (likely not last column), align end to start of backspace button
                            rightToLeftOf(backspaceKey)
                        } else if (column == columnCount - 1) {
                            // last column, align end to end of parent
                            rightOfParent()
                        } else {
                            // neither, align end to start of next view
                            rightToLeftOf(keyViews[i + 1])
                        }
                        matchConstraintPercentWidth = keyWidth
                    })
                }
            }

            Density.Medium, Density.Low -> {
                keyViews.forEachIndexed { i, keyView ->
                    val row = i / columnCount
                    val column = i % columnCount
                    add(keyView, lParams {
                        // layout_constraintTop_to
                        if (row == 0) {
                            // first row, align top to top of parent
                            topOfParent()
                        } else {
                            // not first row, align top to bottom of first view in last row
                            topToBottomOf(keyViews[(row - 1) * columnCount])
                        }
                        // layout_constraintBottom_to
                        if (row == rowCount - 1) {
                            // last row, align bottom to bottom of parent
                            bottomOfParent()
                        } else {
                            // not last row, align bottom to top of first view in next row
                            bottomToTopOf(keyViews[(row + 1) * columnCount])
                        }
                        // layout_constraintLeft_to
                        if (column == 0) {
                            // first column, align start to start of parent
                            leftOfParent()
                        } else {
                            // not first column, align start to end of last column
                            leftToRightOf(keyViews[i - 1])
                        }
                        matchConstraintPercentWidth = keyWidth
                    })
                }
            }
        }
        if (density.showBackspace) {
            add(backspaceKey, lParams {
                // top to bottom of first view of second-last row
                below(keyViews[(rowCount - 2) * columnCount])
                // bottom/right corner
                bottomOfParent()
                rightOfParent()
                matchConstraintPercentWidth = 0.15f
            })
        }
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
    }

    private fun onSymbolClick(str: String) {
        keyActionListener?.onKeyAction(CommitAction(str), Source.Keyboard)
    }

    fun setItems(items: Array<String>) {
        keyViews.forEachIndexed { i, keyView ->
            keyView.apply {
                if (i >= items.size) {
                    isEnabled = false
                    mainText.text = ""
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                    swipeEnabled = false
                    onGestureListener = null
                } else {
                    isEnabled = true
                    val text = items[i]
                    mainText.text = text
                    setOnClickListener {
                        onSymbolClick(text)
                    }
                    setOnLongClickListener { view ->
                        view as KeyView
                        if (!popupOnKeyPress) {
                            // in case "popup on keypress" is disabled, popup keyboard need to know
                            // the actual bounds on press. see [^1] as well
                            view.updateBounds()
                        }
                        // TODO: maybe popup keyboard should just accept String as label?
                        onPopupAction(
                            PopupAction.ShowKeyboardAction(
                                view.id,
                                KeyDef.Popup.Keyboard(text),
                                bounds
                            )
                        )
                        false
                    }
                    swipeEnabled = true
                    onGestureListener = OnGestureListener { view, event ->
                        view as KeyView
                        when (event.type) {
                            CustomGestureView.GestureType.Down -> {
                                if (popupOnKeyPress) {
                                    // [^1]: bounds is first calculated in KeyView's onLayout(), it
                                    // not in screen viewport at the time of layout.
                                    // eg. it's inside the next page of ViewPager
                                    // so update bounds when it's pressed
                                    view.updateBounds()
                                    onPopupAction(
                                        PopupAction.PreviewAction(view.id, text, view.bounds)
                                    )
                                }
                                false
                            }

                            CustomGestureView.GestureType.Move -> {
                                onPopupChangeFocus(view.id, event.x, event.y)
                            }

                            CustomGestureView.GestureType.Up -> {
                                onPopupTrigger(view.id).also {
                                    onPopupAction(PopupAction.DismissAction(view.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onPopupAction(action: PopupAction) {
        popupActionListener?.onPopupAction(action)
    }

    private fun onPopupChangeFocus(viewId: Int, x: Float, y: Float): Boolean {
        val changeFocusAction = PopupAction.ChangeFocusAction(viewId, x, y)
        popupActionListener?.onPopupAction(changeFocusAction)
        return changeFocusAction.outResult
    }

    private fun onPopupTrigger(viewId: Int): Boolean {
        val triggerAction = PopupAction.TriggerAction(viewId)
        // TODO: maybe popup keyboard should just yield String value?
        onPopupAction(triggerAction)
        val action = triggerAction.outAction as? FcitxKeyAction ?: return false
        onSymbolClick(action.act)
        onPopupAction(PopupAction.DismissAction(viewId))
        return true
    }

}
