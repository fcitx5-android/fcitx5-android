package org.fcitx.fcitx5.android.data.theme

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.resource.attrColor
import org.fcitx.fcitx5.android.utils.resource.resColor

object ThemePreset {

    val test = Theme.Builtin(
        backgroundColor = resColor(R.color.red_400),
        barColor = resColor(R.color.red_A700),
        keyBackgroundColor = null,
        keyBackgroundColorBordered = resColor(R.color.key),
        keyTextColor = resColor(R.color.yellow_800),
        keyAltTextColor = resColor(R.color.yellow_500),
        keyAccentBackgroundColor = resColor(R.color.blue_500),
        keyAccentForeground = attrColor(android.R.attr.colorControlHighlight),
        funKeyColor = resColor(R.color.design_default_color_primary_variant),
        dividerColor = resColor(R.color.blue_500),
        isDark = true,
        clipboardEntryColor = resColor(R.color.blue_500)
    )

    // TODO
    val preview = test

}