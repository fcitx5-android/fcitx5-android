/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.ui.R

fun <T : Any> NavController.navigateWithAnim(route: T) {
    navigate(route, navOptions {
        anim {
            enter = R.animator.nav_default_enter_anim
            exit = R.animator.nav_default_exit_anim
            popEnter = R.animator.nav_default_pop_enter_anim
            popExit = R.animator.nav_default_pop_exit_anim
        }
    })
}
