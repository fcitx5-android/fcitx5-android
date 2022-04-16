package org.fcitx.fcitx5.android.utils.resource

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import org.fcitx.fcitx5.android.utils.ContextF
import splitties.resources.color
import splitties.resources.styledColor

typealias IntColor = Int

typealias ColorResource = ContextF<IntColor>

fun resColor(@ColorRes color: Int): ColorResource =
    ColorResource { context -> context.color(color) }

fun attrColor(@AttrRes color: Int): ColorResource =
    ColorResource { context -> context.styledColor(color) }

fun ColorResource.toColorFilter(mode: PorterDuff.Mode) = map { PorterDuffColorFilter(it, mode) }