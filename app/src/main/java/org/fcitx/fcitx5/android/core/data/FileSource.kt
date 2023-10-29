/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

sealed interface FileSource {

    /**
     * This path belongs to app
     */
    object Main : FileSource {
        override fun toString(): String = "Main"
    }

    /**
     * This path belongs to plugin
     */
    data class Plugin(val descriptor: PluginDescriptor) : FileSource
}