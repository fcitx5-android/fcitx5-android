/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodSubtype
import org.fcitx.fcitx5.android.input.FcitxInputMethodService

object InputMethodUtil {
    val serviceName =
        ComponentName(appContext, FcitxInputMethodService::class.java).flattenToShortString()

    fun isEnabled(): Boolean {
        val s = getSecureSettings(Settings.Secure.ENABLED_INPUT_METHODS) ?: return false
        return s.split(':').map { it.substringBefore(';') }.contains(serviceName)
    }

    fun isSelected(): Boolean =
        getSecureSettings(Settings.Secure.DEFAULT_INPUT_METHOD) == serviceName

    fun startSettingsActivity(context: Context) =
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

    fun showPicker() = appContext.inputMethodManager.showInputMethodPicker()

    fun firstVoiceInput(): Pair<String, InputMethodSubtype>? =
        appContext.inputMethodManager
            .shortcutInputMethodsAndSubtypes
            .firstNotNullOfOrNull {
                it.value.find { subType -> subType.mode.lowercase() == "voice" }
                    ?.let { subType -> it.key.id to subType }
            }

    fun switchInputMethod(
        service: FcitxInputMethodService,
        id: String,
        subtype: InputMethodSubtype
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.switchInputMethod(id, subtype)
        } else {
            @Suppress("DEPRECATION")
            appContext.inputMethodManager
                .setInputMethodAndSubtype(service.window.window!!.attributes.token, id, subtype)
        }
    }
}