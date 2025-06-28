/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

fun <T> Array<T>.includes(element: T?): Boolean {
    return indexOf(element) >= 0
}

fun IntArray.includes(element: Int): Boolean {
    return indexOf(element) >= 0
}
