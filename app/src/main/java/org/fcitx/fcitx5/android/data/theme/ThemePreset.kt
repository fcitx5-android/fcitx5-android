package org.fcitx.fcitx5.android.data.theme

import android.graphics.Color
import org.fcitx.fcitx5.android.utils.color

object ThemePreset {

    val PixelLight = Theme.Builtin(
        name = "PixelLight",
        backgroundColor = 0xffeeeeee.color,
        barColor = 0xffeeeeee.color,
        keyboardColor = 0xfffafafa.color,
        keyBackgroundColor = 0xffffffff.color,
        keyTextColor = 0xff212121.color,
        altKeyBackgroundColor = 0xffe1e1e1.color,
        altKeyTextColor = 0xff6e6e6e.color,
        accentKeyBackgroundColor = 0xff4285f4.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x1f000000.color,
        keyShadowColor = 0xffa9abad.color,
        spaceBarColor = 0xffdadada.color,
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
        altKeyTextColor = 0xffacacac.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x33ffffff.color,
        keyShadowColor = 0xff1e2225.color,
        spaceBarColor = 0xff4b4b4b.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0xff464646.color,
        isDark = true
    )

    val PreviewLight = Theme.Builtin(
        name = "PreviewLight",
        backgroundColor = Color.TRANSPARENT.color,
        barColor = 0x5f000000.color,
        keyboardColor = Color.TRANSPARENT.color,
        keyBackgroundColor = 0x32000000.color,
        keyTextColor = 0xff000000.color,
        altKeyBackgroundColor = 0x10000000.color,
        altKeyTextColor = 0xff4564546.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x1f000000.color,
        keyShadowColor = Color.TRANSPARENT.color,
        spaceBarColor = 0xdaffffff.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0x32ffffff.color,
        isDark = false
    )

    val PreviewDark = Theme.Builtin(
        name = "PreviewDark",
        backgroundColor = Color.TRANSPARENT.color,
        barColor = 0x5f000000.color,
        keyboardColor = Color.TRANSPARENT.color,
        keyBackgroundColor = 0x32ffffff.color,
        keyTextColor = 0xffffffff.color,
        altKeyBackgroundColor = 0x10ffffff.color,
        altKeyTextColor = 0xffb9b9bd.color,
        accentKeyBackgroundColor = 0xff5e97f6.color,
        accentKeyTextColor = 0xffffffff.color,
        keyPressHighlightColor = 0x33ffffff.color,
        keyShadowColor = Color.TRANSPARENT.color,
        spaceBarColor = 0xdaffffff.color,
        dividerColor = 0x1fffffff.color,
        clipboardEntryColor = 0x32ffffff.color,
        isDark = true
    )

}