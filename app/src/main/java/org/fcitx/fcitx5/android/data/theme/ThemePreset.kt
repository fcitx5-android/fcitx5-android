package org.fcitx.fcitx5.android.data.theme

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.resource.attrColor
import org.fcitx.fcitx5.android.utils.resource.resColor
import splitties.resources.styledColor

object ThemePreset {

    val app = Theme.Builtin(
        backgroundColor = attrColor(android.R.attr.colorBackground),
        barColor = attrColor(android.R.attr.colorBackground),
        keyBackgroundColor = attrColor(android.R.attr.colorButtonNormal),
        keyBackgroundColorBordered = attrColor(android.R.attr.colorButtonNormal),
        keyTextColor = attrColor(android.R.attr.colorForeground),
        keyAltTextColor = attrColor(android.R.attr.colorForeground),
        keyAccentBackgroundColor = attrColor(android.R.attr.colorAccent),
        keyAccentForeground = attrColor(android.R.attr.colorControlHighlight),
        funKeyColor = attrColor(android.R.attr.colorControlNormal),
        dividerColor = attrColor(android.R.attr.colorButtonNormal),
        lightNavigationBar = {
            ColorUtils.calculateContrast(
                Color.WHITE,
                it.styledColor(android.R.attr.colorBackground)
            ) < 1.5f
        },
        clipboardEntryColor = attrColor(android.R.attr.colorButtonNormal)
    )

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
        lightNavigationBar = { true },
        clipboardEntryColor = resColor(R.color.blue_500)
    )

    // TODO remove this
    val preview = app.copy(
        keyBackgroundColor = null,
        keyBackgroundColorBordered = resColor(R.color.key)
    )

}