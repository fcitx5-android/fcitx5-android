package org.fcitx.fcitx5.android.data.theme

import android.graphics.Color
import org.fcitx.fcitx5.android.utils.ContextF
import org.fcitx.fcitx5.android.utils.resource.ColorResource
import java.io.File

sealed class Theme {

    abstract val barColor: ColorResource

    // override by keyBackgroundColorBordered when key border is enabled
    abstract val keyBackgroundColor: ColorResource?
    abstract val keyBackgroundColorBordered: ColorResource
    abstract val keyTextColor: ColorResource
    abstract val keyAltTextColor: ColorResource
    abstract val keyAccentBackgroundColor: ColorResource
    abstract val keyAccentForeground: ColorResource
    abstract val funKeyColor: ColorResource
    abstract val dividerColor: ColorResource
    abstract val clipboardEntryColor: ColorResource

    // we need context for the default day light theme
    // true -> force light
    // false -> system default
    abstract val lightNavigationBar: ContextF<Boolean>

    data class CustomBackground(
        val backgroundImage: File,
        override val barColor: ColorResource,
        override val keyBackgroundColor: ColorResource?,
        override val keyBackgroundColorBordered: ColorResource,
        override val keyTextColor: ColorResource,
        override val keyAltTextColor: ColorResource,
        override val keyAccentBackgroundColor: ColorResource,
        override val keyAccentForeground: ColorResource,
        override val funKeyColor: ColorResource,
        override val dividerColor: ColorResource,
        override val lightNavigationBar: ContextF<Boolean>,
        override val clipboardEntryColor: ColorResource
    ) : Theme()


    data class Builtin(
        val backgroundColor: ColorResource,
        override val barColor: ColorResource,
        override val keyBackgroundColor: ColorResource?,
        override val keyBackgroundColorBordered: ColorResource,
        override val keyTextColor: ColorResource,
        override val keyAltTextColor: ColorResource,
        override val keyAccentBackgroundColor: ColorResource,
        override val keyAccentForeground: ColorResource,
        override val funKeyColor: ColorResource,
        override val dividerColor: ColorResource,
        override val lightNavigationBar: ContextF<Boolean>,
        override val clipboardEntryColor: ColorResource
    ) : Theme()

    val keyTextColorInverse by lazy {
        keyTextColor.map {
            Color.rgb(
                255 - Color.red(it),
                255 - Color.green(it),
                255 - Color.blue(it)
            )
        }
    }
}