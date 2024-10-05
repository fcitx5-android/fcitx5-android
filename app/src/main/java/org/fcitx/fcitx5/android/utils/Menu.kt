/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.res.ColorStateList
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import splitties.resources.drawable

fun MenuItem.setup(
    @DrawableRes icon: Int,
    @ColorInt iconTint: Int,
    showAsAction: Boolean
): MenuItem {
    if (icon != 0 && iconTint != 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            iconTintList = ColorStateList.valueOf(iconTint)
            setIcon(icon)
        } else {
            setIcon(appContext.drawable(icon)?.apply { setTint(iconTint) })
        }
    }
    if (showAsAction) {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
    return this
}

fun Menu.item(
    @StringRes title: Int,
    @DrawableRes icon: Int = 0,
    @ColorInt iconTint: Int = 0,
    showAsAction: Boolean = false,
    callback: () -> Unit
): MenuItem {
    val item = add(title).setup(icon, iconTint, showAsAction)
    item.setOnMenuItemClickListener {
        callback.invoke()
        true
    }
    return item
}

fun Menu.subMenu(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @ColorInt iconTint: Int,
    showAsAction: Boolean = false,
    setup: SubMenu.() -> Unit
): SubMenu {
    val sub = addSubMenu(title)
    sub.item.setup(icon, iconTint, showAsAction)
    setup.invoke(sub)
    return sub
}
