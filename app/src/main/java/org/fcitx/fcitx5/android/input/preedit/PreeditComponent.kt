/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.preedit

import android.view.View
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.horizontalPadding

class PreeditComponent : UniqueComponent<PreeditComponent>(), Dependent, InputBroadcastReceiver,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val theme by manager.theme()

    val ui by lazy {
        val keyBorder = ThemeManager.prefs.keyBorder.getValue()
        val bkgColor = when (theme) {
            is Theme.Builtin -> if (keyBorder) theme.backgroundColor else theme.barColor
            is Theme.Custom -> theme.backgroundColor
        }
        PreeditUi(context, theme, setupTextView = {
            backgroundColor = bkgColor
            horizontalPadding = dp(8)
        }).apply {
            // TODO make it customizable
            root.alpha = 0.8f
            root.visibility = View.INVISIBLE
        }
    }

    override fun onInputPanelUpdate(data: FcitxEvent.InputPanelEvent.Data) {
        ui.update(data)
        ui.root.visibility = if (ui.visible) View.VISIBLE else View.INVISIBLE
    }
}
