/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.provider.Settings

fun errInvalidType(cls: Class<*>): Nothing {
    throw IllegalArgumentException("Invalid settings type ${cls.name}")
}

inline fun <reified T> getGlobalSettings(name: String): T {
    return when (T::class.java) {
        String::class.java -> Settings.Global.getString(appContext.contentResolver, name)
        Float::class.javaObjectType -> Settings.Global.getFloat(appContext.contentResolver, name, 0f)
        Long::class.javaObjectType -> Settings.Global.getLong(appContext.contentResolver, name, 0L)
        Int::class.javaObjectType -> Settings.Global.getInt(appContext.contentResolver, name, 0)
        else -> errInvalidType(T::class.java)
    } as T
}

inline fun <reified T> getSecureSettings(name: String): T {
    return when (T::class.java) {
        String::class.java -> Settings.Secure.getString(appContext.contentResolver, name)
        Float::class.javaObjectType -> Settings.Secure.getFloat(appContext.contentResolver, name, 0f)
        Long::class.javaObjectType -> Settings.Secure.getLong(appContext.contentResolver, name, 0L)
        Int::class.javaObjectType -> Settings.Secure.getInt(appContext.contentResolver, name, 0)
        else -> errInvalidType(T::class.java)
    } as T
}

inline fun <reified T> getSystemSettings(name: String): T {
    return when (T::class.java) {
        String::class.java -> Settings.System.getString(appContext.contentResolver, name)
        Float::class.javaObjectType -> Settings.System.getFloat(appContext.contentResolver, name, 0f)
        Long::class.javaObjectType -> Settings.System.getLong(appContext.contentResolver, name, 0L)
        Int::class.javaObjectType -> Settings.System.getInt(appContext.contentResolver, name, 0)
        else -> errInvalidType(T::class.java)
    } as T
}
