/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.core

import android.os.Build
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.inputMethodManager

object SubtypeManager {

    private const val MODE_KEYBOARD = "keyboard"

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun syncWith(inputMethods: Array<InputMethodEntry>) {
        val subtypes = Array(inputMethods.size) { i ->
            val im = inputMethods[i]
            InputMethodSubtypeBuilder()
                .setSubtypeId(im.uniqueName.hashCode())
                .setSubtypeNameOverride(im.displayName)
                .setSubtypeLocale(im.languageCode)
                .setSubtypeMode(MODE_KEYBOARD)
                .setIsAsciiCapable(im.uniqueName.startsWith(MODE_KEYBOARD))
                .build()
        }
        val hashCodes = IntArray(subtypes.size) { subtypes[it].hashCode() }
        val imm = appContext.inputMethodManager
        val imiId = InputMethodUtil.serviceName
        // although this method has been marked as deprecated,
        // dynamic subtypes have to be "registered" before they can be "enabled"
        @Suppress("DEPRECATION")
        imm.setAdditionalInputMethodSubtypes(imiId, subtypes)
        imm.setExplicitlyEnabledInputMethodSubtypes(imiId, hashCodes)
    }
}
