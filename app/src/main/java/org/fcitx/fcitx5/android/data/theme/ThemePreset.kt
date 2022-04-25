package org.fcitx.fcitx5.android.data.theme

import android.graphics.Color

object ThemePreset {

    val PixelLight = Theme.Builtin(
        backgroundColor = 0xffeeeeeeu.toInt(),
        barColor = 0xffeeeeeeu.toInt(),
        keyboardColor = 0xfffafafau.toInt(),
        keyBackgroundColor = 0xffffffffu.toInt(),
        keyTextColor = 0xff212121u.toInt(),
        altKeyBackgroundColor = 0xffe1e1e1u.toInt(),
        altKeyTextColor = 0xff6e6e6eu.toInt(),
        accentKeyBackgroundColor = 0xff4285f4u.toInt(),
        accentKeyTextColor = 0xffffffffu.toInt(),
        keyPressHighlightColor = 0x1f000000u.toInt(),
        keyShadowColor = 0xffa9abadu.toInt(),
        spaceBarColor = 0xffdadadau.toInt(),
        dividerColor = 0x1f000000u.toInt(),
        clipboardEntryColor = 0xffffffffu.toInt(),
        isDark = false
    )

    val PixelDark = Theme.Builtin(
        backgroundColor = 0xff2d2d2du.toInt(),
        barColor = 0xff373737u.toInt(),
        keyboardColor = 0xff2d2d2du.toInt(),
        keyBackgroundColor = 0xff464646u.toInt(),
        keyTextColor = 0xfffafafau.toInt(),
        altKeyBackgroundColor = 0xff373737u.toInt(),
        altKeyTextColor = 0xffacacacu.toInt(),
        accentKeyBackgroundColor = 0xff5e97f6u.toInt(),
        accentKeyTextColor = 0xffffffffu.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = 0xff1e2225u.toInt(),
        spaceBarColor = 0xff4b4b4bu.toInt(),
        dividerColor = 0x1fffffffu.toInt(),
        clipboardEntryColor = 0xff464646u.toInt(),
        isDark = true
    )

    val PreviewLight = Theme.Builtin(
        backgroundColor = Color.TRANSPARENT,
        barColor = 0x5f000000,
        keyboardColor = Color.TRANSPARENT,
        keyBackgroundColor = 0x32000000,
        keyTextColor = 0xff000000u.toInt(),
        altKeyBackgroundColor = 0x10000000,
        altKeyTextColor = 0xff4564546u.toInt(),
        accentKeyBackgroundColor = 0xff5e97f6u.toInt(),
        accentKeyTextColor = 0xffffffffu.toInt(),
        keyPressHighlightColor = 0x1f000000u.toInt(),
        keyShadowColor = Color.TRANSPARENT,
        spaceBarColor = 0xdaffffffu.toInt(),
        dividerColor = 0x1fffffffu.toInt(),
        clipboardEntryColor = 0x32ffffff,
        isDark = false
    )

    val PreviewDark = Theme.Builtin(
        backgroundColor = Color.TRANSPARENT,
        barColor = 0x5f000000,
        keyboardColor = Color.TRANSPARENT,
        keyBackgroundColor = 0x32ffffff,
        keyTextColor = 0xffffffffu.toInt(),
        altKeyBackgroundColor = 0x10ffffff,
        altKeyTextColor = 0xffb9b9bdu.toInt(),
        accentKeyBackgroundColor = 0xff5e97f6u.toInt(),
        accentKeyTextColor = 0xffffffffu.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        keyShadowColor = Color.TRANSPARENT,
        spaceBarColor = 0xdaffffffu.toInt(),
        dividerColor = 0x1fffffffu.toInt(),
        clipboardEntryColor = 0x32ffffff,
        isDark = true
    )

}