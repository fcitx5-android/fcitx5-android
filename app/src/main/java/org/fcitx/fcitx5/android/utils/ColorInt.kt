package org.fcitx.fcitx5.android.utils

import androidx.annotation.ColorInt
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ColorInt(@ColorInt val color: Int)

fun colorInt(@ColorInt long: Long) = ColorInt(long.toInt())
fun colorInt(@ColorInt int: Int) = ColorInt(int)

val Int.color
    get() = colorInt(this)

val Long.color
    get() = colorInt(this)
