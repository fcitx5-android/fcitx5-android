package org.fcitx.fcitx5.android.utils

import androidx.annotation.StringRes

class LocalizedRuntimeException
@JvmOverloads constructor(@StringRes message: Int, cause: Throwable? = null) :
    RuntimeException(appContext.getString(message), cause)