/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIcon {
    fun component(context: Context) =
        ComponentName(context.packageName, "${context.packageName}.LauncherAlias")

    fun setHidden(context: Context, hidden: Boolean) {
        val component = component(context)
        val state = if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        if (context.packageManager.getComponentEnabledSetting(component) == state) return
        context.packageManager.setComponentEnabledSetting(
            component, state, PackageManager.DONT_KILL_APP
        )
    }
}
