/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

fun String.isJavaIdentifier(): Boolean {
    if (this.isEmpty()) return false
    if (!this[0].isJavaIdentifierStart()) return false
    for (i in 1..<this.length) {
        if (!this[i].isJavaIdentifierPart()) return false
    }
    return true
}
