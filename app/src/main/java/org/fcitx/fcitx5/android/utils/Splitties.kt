/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.ConstraintLayout
import splitties.experimental.InternalSplittiesApi
import splitties.resources.withResolvedThemeAttribute
import splitties.views.dsl.core.Ui

@OptIn(InternalSplittiesApi::class)
fun Context.styledFloat(@AttrRes attrRes: Int) = withResolvedThemeAttribute(attrRes) {
    when (type) {
        TypedValue.TYPE_FLOAT -> float
        else -> throw IllegalArgumentException("float attribute expected")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun View.styledFloat(@AttrRes attrRes: Int) = context.styledFloat(attrRes)

@Suppress("NOTHING_TO_INLINE")
inline fun Ui.styledFloat(@AttrRes attrRes: Int) = ctx.styledFloat(attrRes)

inline val ConstraintLayout.LayoutParams.unset
    get() = ConstraintLayout.LayoutParams.UNSET
