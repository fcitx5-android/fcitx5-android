/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import timber.log.Timber
import java.lang.reflect.Field

// android.view.View.ScrollabilityCache
private val scrollCacheField by lazy {
    @SuppressLint("DiscouragedPrivateApi")
    View::class.java.getDeclaredField("mScrollCache").apply { isAccessible = true }
}

// android.widget.ScrollBarDrawable
private val scrollBarField by lazy {
    val scrollCacheClass = scrollCacheField.type
    scrollCacheClass.getDeclaredField("scrollBar").apply { isAccessible = true }
}

// android.graphics.drawable.Drawable
private val verticalThumbField by lazy {
    val scrollBarClass = scrollBarField.type
    scrollBarClass.getDeclaredField("mVerticalThumb").apply { isAccessible = true }
}

fun View.setVerticalScrollbarThumbColor(@ColorInt color: Int) {
    val drawable = ShapeDrawable(RectShape()).apply {
        this.paint.color = color
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        verticalScrollbarThumbDrawable = drawable
    } else {
        try {
            val scrollCache = scrollCacheField.get(this)
            val scrollBar = scrollBarField.get(scrollCache)
            verticalThumbField.set(scrollBar, drawable)
        } catch (e: Exception) {
            Timber.w(e, "Cannot set scrollbar color")
        }
    }
}
