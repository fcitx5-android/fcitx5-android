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
    }).apply {
        // disable animation
        animationStyle = 0
    }

    private var isWindowShowing = false

    fun showAt(x: Int, y: Int, w: Int, h: Int) {
        isWindowShowing = true
        if (window.isShowing) {
            window.update(x, y, w, h)
        } else {
            window.width = w
            window.height = h
            window.showAtLocation(contentView, Gravity.TOP or Gravity.START, x, y)
        }
    }

    private val cachedLocation = intArrayOf(0, 0)

    fun show() {
        val (x, y) = cachedLocation.also { contentView.getLocationInWindow(it) }
        val width = contentView.width
        val height = contentView.height
        showAt(x, y, width, height)
    }

    fun dismiss() {
        if (isWindowShowing) {
            isWindowShowing = false
            window.dismiss()
        }
    }
}
