/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

object ThemePreset {

    val MaterialLight = Theme.Builtin(
        name = "MaterialLight",
        isDark = false,
        backgroundColor = 0xffeceff1,
        barColor = 0xffe4e7e9,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0xfffbfbfc,
        keyTextColor = 0xff37474f,
        altKeyBackgroundColor = 0xffdfe2e4,
        // Google Pinyin's symbol color on alphabet key: #727d82
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

    val MaterialDark = Theme.Builtin(
        name = "MaterialDark",
        isDark = true,
        backgroundColor = 0xff263238,
        barColor = 0xff21272b,
        keyboardColor = 0xff263238,
        keyBackgroundColor = 0xff404a50,
        keyTextColor = 0xffd9dbdc,
        altKeyBackgroundColor = 0xff313c42,
        // Google Pinyin's symbol color on alphabet key: #b3b7b9
        altKeyTextColor = 0xffadb1b3,
        accentKeyBackgroundColor = 0xff6eaca8,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff1f292e,
        popupBackgroundColor = 0xff3c474c,
        popupTextColor = 0xffffffff,
        spaceBarColor = 0xff3b464c,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff404a50,
        genericActiveBackgroundColor = 0xff4db6ac,
        genericActiveForegroundColor = 0xffffffff
    )

    val PixelLight = Theme.Builtin(
        name = "PixelLight",
        isDark = false,
        backgroundColor = 0xffeeeeee,
        barColor = 0xffeeeeee,
        keyboardColor = 0xfffafafa,
        keyBackgroundColor = 0xffffffff,
        keyTextColor = 0xff212121,
        altKeyBackgroundColor = 0xffe1e1e1,
        // Google Pinyin's symbol color on alphabet key: #4e4e4e
        altKeyTextColor = 0xff6e6e6e,
        accentKeyBackgroundColor = 0xff4285f4,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        keyShadowColor = 0xffc2c2c2,
        popupBackgroundColor = 0xffeeeeee,
        popupTextColor = 0xff212121,
        spaceBarColor = 0xffdbdbdb,
        dividerColor = 0x1f000000,
        clipboardEntryColor = 0xffffffff,
        genericActiveBackgroundColor = 0xff5e97f6,
        genericActiveForegroundColor = 0xffffffff
    )

    val PixelDark = Theme.Builtin(
        name = "PixelDark",
        isDark = true,
        backgroundColor = 0xff2d2d2d,
        barColor = 0xff373737,
        keyboardColor = 0xff2d2d2d,
        keyBackgroundColor = 0xff464646,
        keyTextColor = 0xfffafafa,
        altKeyBackgroundColor = 0xff373737,
        // Google Pinyin's symbol color on alphabet key: #d6d6d6
        altKeyTextColor = 0xffacacac,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff252525,
        popupBackgroundColor = 0xff373737,
        popupTextColor = 0xfffafafa,
        spaceBarColor = 0xff4a4a4a,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff464646,
        genericActiveBackgroundColor = 0xff5e97f6,
        genericActiveForegroundColor = 0xfffafafa
    )

    val DeepBlue = Theme.Builtin(
        name = "DeepBlue",
        isDark = true,
        backgroundColor = 0xff1565c0,
        barColor = 0xff0d47a1,
        keyboardColor = 0xff1565c0,
        keyBackgroundColor = 0xff3f80cb,
        keyTextColor = 0xffffffff,
        altKeyBackgroundColor = 0xff2771c4,
        // Google Pinyin's symbol color on alphabet key: #d6d6d6
        altKeyTextColor = 0xffa9c6e7,
        accentKeyBackgroundColor = 0xff2196f3,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff1255a1,
        popupBackgroundColor = 0xff0d47a1,
        popupTextColor = 0xffffffff,
        spaceBarColor = 0xff7eaadc,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff3f80cb,
        genericActiveBackgroundColor = 0xff094cea,
        genericActiveForegroundColor = 0xffffffff
    )

    val AMOLEDBlack = Theme.Builtin(
        name = "AMOLEDBlack",
        isDark = true,
        backgroundColor = 0xff000000,
        barColor = 0xff373737,
        keyboardColor = 0xff000000,
        keyBackgroundColor = 0xff2e2e2e,
        keyTextColor = 0xffffffff,
        altKeyBackgroundColor = 0xff141414,
        // Google Pinyin's symbol color on alphabet key: #d9e6f5
        altKeyTextColor = 0xffa1a1a1,
        accentKeyBackgroundColor = 0xff80cbc4,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff000000,
        popupBackgroundColor = 0xff373737,
        popupTextColor = 0xffffffff,
        spaceBarColor = 0xff727272,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff2e2e2e,
        genericActiveBackgroundColor = 0xff26a69a,
        genericActiveForegroundColor = 0xffffffff
    )

    val NordLight = Theme.Builtin(
        name = "NordLight",
        isDark = false,
        backgroundColor = 0xffD8DEE9,
        barColor = 0xffE5E9F0,
        keyboardColor = 0xffECEFF4,
        keyBackgroundColor = 0xffECEFF4,
        keyTextColor = 0xff2E3440,
        altKeyBackgroundColor = 0xffE5E9F0,
        altKeyTextColor = 0xff434C5E,
        accentKeyBackgroundColor = 0xff5E81AC,
        accentKeyTextColor = 0xffECEFF4,
        keyPressHighlightColor = 0x1f000000,
        keyShadowColor = 0x1f000000,
        popupBackgroundColor = 0xffE5E9F0,
        popupTextColor = 0xff2E3440,
        spaceBarColor = 0xffD8DEE9,
        dividerColor = 0x1f000000,
        clipboardEntryColor = 0xffECEFF4,
        genericActiveBackgroundColor = 0xff5E81AC,
        genericActiveForegroundColor = 0xffECEFF4
    )

    val NordDark = Theme.Builtin(
        name = "NordDark",
        isDark = true,
        backgroundColor = 0xff2E3440,
        barColor = 0xff434C5E,
        keyboardColor = 0xff2E3440,
        keyBackgroundColor = 0xff4C566A,
        keyTextColor = 0xffECEFF4,
        altKeyBackgroundColor = 0xff3B4252,
        altKeyTextColor = 0xffD8DEE9,
        accentKeyBackgroundColor = 0xff88C0D0,
        accentKeyTextColor = 0xff2E3440,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff434C5E,
        popupBackgroundColor = 0xff434C5E,
        popupTextColor = 0xffECEFF4,
        spaceBarColor = 0xff4C566A,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff4C566A,
        genericActiveBackgroundColor = 0xff88C0D0,
        genericActiveForegroundColor = 0xff2E3440
    )

    val Monokai = Theme.Builtin(
        name = "Monokai",
        isDark = true,
        backgroundColor = 0xff272822,
        barColor = 0xff1f201b,
        keyboardColor = 0xff272822,
        keyBackgroundColor = 0xff33342c,
        keyTextColor = 0xffd6d6d6,
        altKeyBackgroundColor = 0xff2d2e27,
        altKeyTextColor = 0xff797979,
        accentKeyBackgroundColor = 0xffb05279,
        accentKeyTextColor = 0xffd6d6d6,
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff171813,
        popupBackgroundColor = 0xff1f201b,
        popupTextColor = 0xffd6d6d6,
        spaceBarColor = 0xff373830,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0xff33342c,
        genericActiveBackgroundColor = 0xffb05279,
        genericActiveForegroundColor = 0xffd6d6d6
    )

    /**
     * transparent background with semi-transparent white keys
     */
    val TransparentDark = Theme.Builtin(
        name = "TransparentDark",
        isDark = true,
        backgroundColor = 0xff2d2d2d,
        barColor = 0x4c000000,
        keyboardColor = 0x00000000,
        keyBackgroundColor = 0x4bffffff,
        keyTextColor = 0xffffffff,
        altKeyBackgroundColor = 0x0cffffff,
        altKeyTextColor = 0xc9ffffff,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        keyShadowColor = 0x00000000,
        popupBackgroundColor = 0xff373737,
        popupTextColor = 0xfffafafa,
        spaceBarColor = 0x4bffffff,
        dividerColor = 0x1fffffff,
        clipboardEntryColor = 0x32ffffff,
        genericActiveBackgroundColor = 0xff5e97f6,
        genericActiveForegroundColor = 0xfffafafa
    )

    /**
     * transparent background with semi-transparent black keys
     */
    val TransparentLight = Theme.Builtin(
        name = "TransparentLight",
        isDark = false,
        backgroundColor = 0xffeeeeee,
        barColor = 0x26000000,
        keyboardColor = 0x00000000,
        keyBackgroundColor = 0x4bffffff,
        keyTextColor = 0xff000000,
        altKeyBackgroundColor = 0x0cffffff,
        altKeyTextColor = 0xb9000000,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        keyShadowColor = 0x00000000,
        popupBackgroundColor = 0xffeeeeee,
        popupTextColor = 0xff212121,
        spaceBarColor = 0x5affffff,
        dividerColor = 0x1f000000,
        clipboardEntryColor = 0x4bffffff,
        genericActiveBackgroundColor = 0xff5e97f6,
        genericActiveForegroundColor = 0xffffffff
    )

}