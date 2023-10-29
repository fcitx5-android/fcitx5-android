/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

sealed interface PluginLoadFailed {
    data class PathConflict(
        val plugin: PluginDescriptor,
        val path: String,
        val existingSrc: FileSource
    ) : PluginLoadFailed

    data object MissingPluginDescriptor : PluginLoadFailed

    data object PluginDescriptorParseError : PluginLoadFailed

    data class MissingDataDescriptor(
        val plugin: PluginDescriptor
    ) : PluginLoadFailed

    data class DataDescriptorParseError(
        val plugin: PluginDescriptor
    ) : PluginLoadFailed

    data class PluginAPIIncompatible(
        val api: String
    ) : PluginLoadFailed
}