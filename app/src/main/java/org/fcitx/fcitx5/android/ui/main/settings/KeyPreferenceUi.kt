package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.utils.KeyUtils
import splitties.resources.styledDrawable
import splitties.views.dsl.core.*
import splitties.views.imageResource

class KeyPreferenceUi(override val ctx: Context) : Ui {

    private val textView = textView { }

    private inner class CheckBoxRow(text: String, val state: KeyState) : Ui {
        val textView = textView {
            this.text = text
        }
        val checkBox = checkBox {
            setOnCheckedChangeListener { _, _ ->
                update()
            }
        }
        override val ctx: Context
            get() = this@KeyPreferenceUi.ctx
        override val root = horizontalLayout {
            add(textView, lParams())
            add(checkBox, lParams())
        }
    }

    private val ctrl = CheckBoxRow("Control", KeyState.Ctrl)
    private val alt = CheckBoxRow("Alt", KeyState.Alt)
    private val shift = CheckBoxRow("Shift", KeyState.Shift)
    private val input = editText {
        inputType = EditorInfo.TYPE_NULL
        setOnKeyListener { _, _, event ->
            if (event.action != KeyEvent.ACTION_DOWN)
                return@setOnKeyListener false
            val sym = KeySym.fromKeyEvent(event)
                ?: return@setOnKeyListener false
            keySym = sym
            val states = KeyStates.fromKeyEvent(event)
            update(states) ?: return@setOnKeyListener false
            return@setOnKeyListener true
        }
    }
    private val clearButton = imageButton {
        background = styledDrawable(android.R.attr.selectableItemBackground)
        imageResource = R.drawable.ic_baseline_delete_24
        setOnClickListener {
            setKey(Key.none)
        }
    }


    override val root = verticalLayout {
        add(textView, lParams())
        add(ctrl.root, lParams())
        add(alt.root, lParams())
        add(shift.root, lParams())
        add(input, lParams())
        add(clearButton, lParams())
    }

    fun setKey(key: Key) {
        lastKey = key
        keySym = key.keySym
        textView.text = key.showKeyString()
        ctrl.checkBox.isChecked = key.keyStates.ctrl
        alt.checkBox.isChecked = key.keyStates.alt
        shift.checkBox.isChecked = key.keyStates.shift
    }

    private val keyStates
        get() = KeyStates(
            *listOf(ctrl, alt, shift)
                .mapNotNull { it.takeIf { it.checkBox.isChecked }?.state }
                .toTypedArray()
        )

    private var keySym: KeySym? = null

    private fun check(states: KeyStates = keyStates): Key? =
        keySym?.let { KeyUtils.createKey(it, states) }

    private fun update(states: KeyStates = keyStates) =
        check(states)?.let { setKey(it) }

    var lastKey: Key = Key.none
        private set
}