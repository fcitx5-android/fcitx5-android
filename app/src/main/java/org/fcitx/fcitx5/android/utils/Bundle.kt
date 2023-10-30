package org.fcitx.fcitx5.android.utils

import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    @Suppress("DEPRECATION")
    return getSerializable(key) as? T
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        getSerializable(key, T::class.java)
//    } else {
//        @Suppress("DEPRECATION")
//        getSerializable(key) as? T
//    }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    // https://issuetracker.google.com/issues/240585930#comment6
    @Suppress("DEPRECATION")
    return getParcelable(key) as? T
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        getParcelable(key, T::class.java)
//    } else {
//        @Suppress("DEPRECATION")
//        getParcelable(key) as? T
//    }
}

inline fun <reified T : Parcelable> Bundle.parcelableArray(key: String): Array<T>? {
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    return getParcelableArray(key) as? Array<T>
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        getParcelableArray(key, T::class.java)
//    } else {
//        @Suppress("DEPRECATION", "UNCHECKED_CAST")
//        getParcelableArray(key) as? Array<T>
//    }
}
