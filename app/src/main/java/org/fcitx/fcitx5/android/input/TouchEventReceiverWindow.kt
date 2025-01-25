/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow

class TouchEventReceiverWindow(
    private val contentView: View
) {
    private val ctx = contentView.context

    private val window = PopupWindow(object : View(ctx) {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            return contentView.dispatchTouchEvent(event)
        }
    })

    private var isWindowShowing = false

    fun showup() {
        isWindowShowing = true
        val location = intArrayOf(0, 0)
        contentView.getLocationInWindow(location)
        val (x, y) = location
        val width = contentView.width
        val height = contentView.height
        if (window.isShowing) {
            window.update(x, y, width, height)
        } else {
            window.width = width
            window.height = height
            window.showAtLocation(contentView, Gravity.NO_GRAVITY, x, y)
        }
    }

    fun dismiss() {
        if (isWindowShowing) {
            isWindowShowing = false
            window.dismiss()
        }
    }
}