/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.theme.Theme
import android.view.MotionEvent
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import splitties.dimensions.dp
import timber.log.Timber
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class NumberRow(ctx: Context, theme: Theme) : BaseKeyboard(ctx, theme, Layout) {

    private var gestureStartEvent: MotionEvent? = null
    private var collapseGestureTriggered: Boolean = false

    var onCollapseListener: (() -> Unit)? = null

    private fun checkGesture(ev: MotionEvent): Boolean {
        val startEvent = gestureStartEvent ?: return false
        val firstPointerId = startEvent.getPointerId(startEvent.actionIndex)
        if (ev.getPointerId(ev.actionIndex) == firstPointerId) {
            val dir = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_LTR) 1 else -1
            val sx = startEvent.x * dir
            val cx = ev.getX(ev.actionIndex) * dir
            val shouldCollapse = cx > sx && abs(cx - sx) > dp(KawaiiBarComponent.HEIGHT)
            if (shouldCollapse) {
                Timber.d("NumberRow: intercepted gesture from child keyboard to handle swipe")
                resetState()
                collapseGestureTriggered = true
                return true
            }
        }
        return false
    }

    private fun resetState() {
        gestureStartEvent?.recycle()
        gestureStartEvent = null
        collapseGestureTriggered = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestureStartEvent = MotionEvent.obtain(ev)
            MotionEvent.ACTION_MOVE -> {
                if (checkGesture(ev)) return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> resetState()
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestureStartEvent = MotionEvent.obtain(event)
            MotionEvent.ACTION_MOVE -> checkGesture(event)
            MotionEvent.ACTION_UP -> {
                if (collapseGestureTriggered) {
                    resetState()
                    onCollapseListener?.invoke()
                    handled = true
                }
            }
            MotionEvent.ACTION_CANCEL -> resetState()
        }
        return super.onTouchEvent(event) || handled
    }

    companion object {
        val Layout = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
                KeyDef(
                    KeyDef.Appearance.Text(
                        displayText = digit,
                        textSize = 21f,
                        border = KeyDef.Appearance.Border.Off,
                        margin = false
                    ),
                    setOf(
                        KeyDef.Behavior.Press(KeyAction.SymAction(KeySym(digit.codePointAt(0))))
                    ),
                    arrayOf(KeyDef.Popup.Preview(digit))
                )
            }
        )
    }
}
