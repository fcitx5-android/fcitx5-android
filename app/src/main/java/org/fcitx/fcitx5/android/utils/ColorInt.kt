package org.fcitx.fcitx5.android.utils

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ColorInt(@ColorInt val color: Int) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(color)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<org.fcitx.fcitx5.android.utils.ColorInt> {
        override fun createFromParcel(parcel: Parcel): org.fcitx.fcitx5.android.utils.ColorInt {
            return ColorInt(parcel)
        }

        override fun newArray(size: Int): Array<org.fcitx.fcitx5.android.utils.ColorInt?> {
            return arrayOfNulls(size)
        }
    }
}

fun colorInt(@ColorInt long: Long) = ColorInt(long.toInt())
fun colorInt(@ColorInt int: Int) = ColorInt(int)

val Int.color
    get() = colorInt(this)

val Long.color
    get() = colorInt(this)
