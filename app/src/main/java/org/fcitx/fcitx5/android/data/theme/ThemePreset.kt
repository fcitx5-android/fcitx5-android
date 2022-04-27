package org.fcitx.fcitx5.android.data.theme

import android.graphics.Color
import org.fcitx.fcitx5.android.utils.color

object ThemePreset {

    val MaterialLight = Theme.Builtin(
        name = "MaterialLight",
        backgroundColor = 0xffeceff1.color,
        barColor = 0xffe4e7e9.color,
        keyboardColor = 0xffeceff1.color,
        keyBackgroundColor = 0xfffbfbfc.color,
        keyTextColor = 0xff37474f.color,
        altKeyBackgroundColor = 0xffdfe2e4.color,
        // Google Pinyin's symbol color on alphabet key: #727d82
        altKeyTextColor = 0xff7a858a.color,
        accentKeyBackgroundColor = 0xff5cb5ab.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x1f000000.color,
        keyShadowColor = 0xffc0c3c4.color,
        spaceBarColor = 0xffc9ced1.color,
        dividerColor = 0x1f000000.color,
        clipboardEntryColor = 0xffffffff.color,
        isDark = false
    )

    val MaterialDark = Theme.Builtin(
        name = "MaterialDark",
        backgroundColor = 0xff263238.color,
        barColor = 0xff21272b.color,
        keyboardColor = 0xff263238.color,
        keyBackgroundColor = 0xff404a50.color,
        keyTextColor = 0xffd9dbdc.color,
        altKeyBackgroundColor = 0xff313c42.color,
        // Google Pinyin's symbol color on alphabet key: #b3b7b9
        altKeyTextColor = 0xffadb1b3.color,
        accentKeyBackgroundColor = 0xff6eaca8.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x33ffffff.color,
        keyShadowColor = 0xff1f292e.color,
        spaceBarColor = 0xff3b464c.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0xff464646.color,
        isDark = true
    )

    val PixelLight = Theme.Builtin(
        name = "PixelLight",
        backgroundColor = 0xffeeeeee.color,
        barColor = 0xffeeeeee.color,
        keyboardColor = 0xfffafafa.color,
        keyBackgroundColor = 0xffffffff.color,
        keyTextColor = 0xff212121.color,
        altKeyBackgroundColor = 0xffe1e1e1.color,
        // Google Pinyin's symbol color on alphabet key: #4e4e4e
        altKeyTextColor = 0xff6e6e6e.color,
        accentKeyBackgroundColor = 0xff4285f4.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x1f000000.color,
        keyShadowColor = 0xffc2c2c2.color,
        spaceBarColor = 0xffdbdbdb.color,
        dividerColor = 0x1f000000.color,
        clipboardEntryColor = 0xffffffff.color,
        isDark = false
    )

    val PixelDark = Theme.Builtin(
        name = "PixelDark",
        backgroundColor = 0xff2d2d2d.color,
        barColor = 0xff373737.color,
        keyboardColor = 0xff2d2d2d.color,
        keyBackgroundColor = 0xff464646.color,
        keyTextColor = 0xfffafafa.color,
        altKeyBackgroundColor = 0xff373737.color,
        // Google Pinyin's symbol color on alphabet key: #d6d6d6
        altKeyTextColor = 0xffacacac.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x33ffffff.color,
        keyShadowColor = 0xff252525.color,
        spaceBarColor = 0xff4a4a4a.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0xff464646.color,
        isDark = true
    )

    /**
     * transparent background with semi-transparent white keys
     */
    val TransparentDark = Theme.Builtin(
        name = "TransparentDark",
        backgroundColor = Color.TRANSPARENT.color,
        barColor = 0x5f000000.color,
        keyboardColor = Color.TRANSPARENT.color,
        keyBackgroundColor = 0x4bffffff.color,
        keyTextColor = 0xffffffff.color,
        altKeyBackgroundColor = 0x0cffffff.color,
        altKeyTextColor = 0xc9ffffff.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x1f000000.color,
        keyShadowColor = Color.TRANSPARENT.color,
        spaceBarColor = 0x4bffffff.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0x32ffffff.color,
        isDark = true
    )

    /**
     * transparent background with semi-transparent black keys
     */
    val TransparentLight = Theme.Builtin(
        name = "TransparentLight",
        backgroundColor = Color.TRANSPARENT.color,
        barColor = 0x5f000000.color,
        keyboardColor = Color.TRANSPARENT.color,
        keyBackgroundColor = 0x32000000.color,
        keyTextColor = 0xff000000.color,
        altKeyBackgroundColor = 0x10000000.color,
        altKeyTextColor = 0xb9000000.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x33ffffff.color,
        keyShadowColor = Color.TRANSPARENT.color,
        spaceBarColor = 0x32000000.color,
        dividerColor = 0x1f000000.color,
        clipboardEntryColor = 0x32000000.color,
        isDark = false
    )

}