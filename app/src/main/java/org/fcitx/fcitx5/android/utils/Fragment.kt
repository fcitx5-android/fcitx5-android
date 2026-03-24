/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute

fun <T : Any> Fragment.navigateWithAnim(route: T) {
    findNavController().navigateWithAnim(route)
}

inline fun <reified T : Any> Fragment.lazyRoute() = lazy {
    findNavController().getBackStackEntry<T>().toRoute<T>()
}
