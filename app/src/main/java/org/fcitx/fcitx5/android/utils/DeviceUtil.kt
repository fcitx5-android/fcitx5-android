/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.os.Build

object DeviceUtil {

    val isMIUI: Boolean by lazy {
        getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
    }

    /**
     * https://www.cnblogs.com/qixingchao/p/15899405.html
     */
    val isHMOS: Boolean by lazy {
        getSystemProperty("hw_sc.build.platform.version").isNotEmpty()
    }

    /**
     * https://stackoverflow.com/questions/60122037/how-can-i-detect-samsung-one-ui
     */
    val isSamsungOneUI: Boolean by lazy {
        try {
            val semPlatformInt = Build.VERSION::class.java
                .getDeclaredField("SEM_PLATFORM_INT")
                .getInt(null)
            semPlatformInt > 90000
        } catch (e: Exception) {
            false
        }
    }

    val isVivoOriginOS: Boolean by lazy {
        getSystemProperty("ro.vivo.os.version").isNotEmpty()
    }

}
