/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates

import android.content.Context
import android.widget.PopupMenu
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import splitties.resources.styledColor
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter

class CandidateItemUi(override val ctx: Context, theme: Theme) : Ui {

    val text = view(::AutoScaleTextView) {
        scaleMode = AutoScaleTextView.Mode.Proportional
        textSize = 20f // sp
        isSingleLine = true
        gravity = gravityCenter
        setTextColor(theme.keyTextColor)
    }

    override val root = view(::CustomGestureView) {
        background = pressHighlightDrawable(theme.keyPressHighlightColor)

        add(text, lParams(wrapContent, matchParent) {
            gravity = gravityCenter
        })
    }

    private var promptMenu: PopupMenu? = null

    fun showExtraActionMenu(currentIm: InputMethodEntry, onForget: () -> Unit) {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(ctx, root).apply {
            val actions = arrayListOf<Pair<String, () -> Unit>>().apply {
                // only pinyin and table could forget words
                if (currentIm.addon == "pinyin" || currentIm.addon == "table") {
                    add(ctx.getString(R.string.action_forget_candidate_word) to onForget)
                }
            }
            menu.apply {
                add(buildSpannedString {
                    bold {
                        color(ctx.styledColor(android.R.attr.colorAccent)) {
                            append(text.text.toString())
                        }
                    }
                }).apply {
                    isEnabled = false
                }
                actions.forEach { (title, action) ->
                    add(title).setOnMenuItemClickListener {
                        action()
                        true
                    }
                }
                if (actions.isEmpty()) {
                    add(R.string.no_action_available).apply {
                        isEnabled = false
                    }
                }
                add(android.R.string.cancel).apply {
                    setOnMenuItemClickListener { true }
                }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }
}
