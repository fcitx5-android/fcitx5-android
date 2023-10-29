/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.wm

/**
 * An empty interface marks that the instance and view of the window will be kept in the window manager,
 * not removing from the scope. This is useful when we want to initialize a window's view only once, e.g. for the keyboard window.
 * Usually, essential windows should be initialized and added to the scope in [org.fcitx.fcitx5.android.input.InputView].
 */
interface EssentialWindow {
    interface Key

    /**
     * Since the window is saved in the window manager,
     * we need a key to discriminate between other essential windows
     */
    val key: Key

    /**
     * Before the window was added to window manager's layout
     */
    fun beforeAttached() {}
}