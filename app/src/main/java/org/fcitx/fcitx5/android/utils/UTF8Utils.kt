package org.fcitx.fcitx5.android.utils

import com.sun.jna.Library

interface UTF8Utils : Library {

    fun validateUTF8(str: String): Boolean

    companion object {
        val instance: UTF8Utils by nativeLib("utf8utils")
    }

}