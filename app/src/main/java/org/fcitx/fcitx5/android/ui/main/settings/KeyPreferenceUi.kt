/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.button
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageDrawable

class KeyPreferenceUi(override val ctx: Context) : Ui {

    private val textView = textView {
        gravity = gravityCenter
    }

    private inner class ModifierButton(label: String, val modifier: KeyState) : Ui {
        override val ctx = this@KeyPreferenceUi.ctx

        override val root = button {
            text = label
            isAllCaps = false
            // those 2 are both necessary, don't ask me why ´_>`
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                checked = !checked
                setKey(Key.create(keySym, keyStates))
            }
        }

        var checked: Boolean = false
            set(value) {
                field = value
                applyStyles()
            }

        fun applyStyles() = root.apply {
            backgroundTintList = ctx.styledColorSL(
                if (checked) android.R.attr.colorAccent else android.R.attr.colorBackgroundFloating
            )
            setTextColor(
                ctx.styledColor(
                    if (checked) android.R.attr.colorForegroundInverse else android.R.attr.colorForeground
                )
            )
        }
    }

    private val modifierButtons = arrayOf(
        ModifierButton("Ctrl", KeyState.Ctrl),
        ModifierButton("Alt", KeyState.Alt),
        ModifierButton("Shift", KeyState.Shift)
    )

    private val input = editText {
        textSize = 16f // sp
        inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_FLAG_FORCE_ASCII
        privateImeOptions = FcitxInputMethodService.DeleteSurroundingFlag
        requestFocus()
        setOnKeyListener { _, _, event ->
            if (event.action != KeyEvent.ACTION_DOWN)
                return@setOnKeyListener false
            var sym = KeySym.fromKeyEvent(event)
                ?: return@setOnKeyListener false
            // convert lowercase latin to uppercase
            if (sym.sym in 0x61..0x7a) {
                sym = KeySym(sym.sym - 0x20)
            }
            val states = KeyStates.fromKeyEvent(event)
            val newKey = Key.create(sym, states)
            if (newKey.sym == Key.None.sym)
                return@setOnKeyListener false
            setKey(newKey)
            return@setOnKeyListener true
        }
        addTextChangedListener {
            val text = it?.toString() ?: return@addTextChangedListener
            val input = Key.parse(text)
            setKey(Key.create(input.keySym, keyStates))
        }
    }

    private val clearButton = imageButton {
        background = styledDrawable(android.R.attr.actionBarItemBackground)
        imageDrawable = drawable(R.drawable.ic_baseline_delete_24)!!.apply {
            setTint(styledColor(android.R.attr.colorControlNormal))
        }
        setOnClickListener {
            setKey(Key.None)
        }
    }

    override val root = constraintLayout {
        val vMargin = dp(18)
        val hMargin = dp(24)
        add(textView, lParams(matchConstraints, wrapContent) {
            topOfParent(vMargin)
            startOfParent(hMargin)
            before(clearButton)
            above(modifierButtons.first().root)
        })
        val iconSize = dp(48)
        add(clearButton, lParams(iconSize, iconSize) {
            below(textView)
            above(textView)
            endOfParent(hMargin)
        })
        modifierButtons.forEachIndexed { i, btn ->
            add(btn.root, lParams(matchConstraints, wrapContent) {
                below(textView, vMargin)
                if (i == 0) startOfParent(hMargin) else after(modifierButtons[i - 1].root)
                bottomOfParent(vMargin)
            })
        }
        add(input, lParams(matchConstraints, wrapContent) {
            below(textView, vMargin)
            after(modifierButtons.last().root)
            endOfParent(hMargin)
            bottomOfParent(vMargin)
        })
    }

    fun setKey(key: Key) {
        lastKey = key
        keySym = key.keySym
        textView.text = key.localizedString.ifEmpty { ctx.getString(R.string.none) }
        modifierButtons.forEach {
            it.checked = key.keyStates.has(it.modifier)
        }
        val symText = key.localizedString.substringAfterLast('+')
        input.hint = if (ModifierRegex.containsMatchIn(symText)) "" else symText
    }

    private val keyStates
        get() = KeyStates(
            *modifierButtons
                .mapNotNull { it.takeIf { it.checked }?.modifier }
                .toTypedArray()
        )

    private var keySym = KeySym(0)

    var lastKey: Key = Key.None
        private set

    companion object {
        val ModifierRegex = Regex("Control|Alt|Shift")
    }
}