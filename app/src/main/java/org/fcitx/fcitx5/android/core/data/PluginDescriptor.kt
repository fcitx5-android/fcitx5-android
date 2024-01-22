/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.core.data.PluginDescriptor.Companion.pluginPackagePrefix

/**
 * Metadata of a plugin, at `res/xml/plugin.xml`
 */
data class PluginDescriptor(
    /**
     * Must have [pluginPackagePrefix] prefix and end with `.debug` if it's debug variant
     */
    val packageName: String,
    /**
     * For future incompatible updates
     */
    val apiVersion: String,
    /**
     * May provide gettext domain
     */
    val domain: String?,
    /**
     * Can use string resource, e.g. `@string/description`
     */
    val description: String,
    /**
     * Contains IPC service with action `${mainApplicationId}.plugin.SERVICE`. Default to `false`.
     */
    val hasService: Boolean,
    val versionName: String,
    val nativeLibraryDir: String,
    val libraryDependency: Map<String, List<String>>
) {
    val name = packageName.removePrefix(pluginPackagePrefix).removeSuffix(pluginPackageSuffix)

    companion object {
        const val pluginPackagePrefix = "org.fcitx.fcitx5.android.plugin."
        const val pluginPackageSuffix = ".${BuildConfig.BUILD_TYPE}"
        const val pluginAPI = "0.1"
    }
}