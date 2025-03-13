/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data.theme

import android.os.Build
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.utils.appContext

class ThemeDynamic {
    @RequiresApi(Build.VERSION_CODES.S)
    val MaterialYou = Theme.Builtin(
        name = "MaterialYou",
        isDark = false,
        backgroundColor = appContext.getColor(android.R.color.system_accent1_100),
        barColor = 0xffe4e7e9,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0xfffbfbfc,
        keyTextColor = 0xff37474f,
        candidateTextColor = 0xff37474f,
        candidateLabelColor = 0xff37474f,
        candidateCommentColor = 0xff7a858a,
        altKeyBackgroundColor = 0xffdfe2e4,
        altKeyTextColor = 0xff7a858a,
        accentKeyBackgroundColor = 0xff5cb5ab,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        keyShadowColor = 0xffc0c3c4,
        popupBackgroundColor = 0xffd9dbdd,
        popupTextColor = 0xff37474f,
        spaceBarColor = 0xffc9ced1,
        dividerColor = 0x1f000000,
        clipboardEntryColor = 0xfffbfbfc,
        genericActiveBackgroundColor = 0xff80cbc4,
        genericActiveForegroundColor = 0xff37474f
    )
}

object ThemeDynamicProvider {
    var current = ThemeDynamic()
}