/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input

import android.content.Context

class PackageNameCache(private val ctx: Context) : HashMap<Int, String>() {
    fun forUid(uid: Int): String {
        val cached = get(uid)
        if (cached != null) return cached
        // returns "${sharedUserIdName}:${uid}" rather than package name if app uses sharedUserId
        val name = ctx.packageManager.getNameForUid(uid)
        if (name != null) {
            // strip :uid to make it constant across devices
            val pkgName = name.substringBeforeLast(':')
            put(uid, pkgName)
            return pkgName
        }
        return uid.toString()
    }
}
