/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
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
    /**
     * Provides an interactive input panel (e.g. handwriting) with IPC service
     * action `${mainApplicationId}.plugin.INTERACTIVE_PANEL`. Default to `false`.
     */
    val hasInteractivePanel: Boolean,
    /**
     * Panel components (as declared in `<panelComponents>` of `plugin.xml`)
     * the plugin provides. An interactive input panel is a keyboard-like
     * surface, so a panel plugin is expected to provide the components listed
     * in [Companion.requiredPanelComponents]. Empty when not declared.
     */
    val panelComponents: Set<String> = emptySet(),
    /**
     * Language of the panel (as declared in `<panelLanguage>` of `plugin.xml`),
     * e.g. `zh` for a handwriting panel. The panel is registered as an input
     * method entry of this language. Empty when not declared.
     */
    val panelLanguage: String = "",
    val versionName: String,
    val nativeLibraryDir: String
) {
    val name = packageName.removePrefix(pluginPackagePrefix).removeSuffix(pluginPackageSuffix)

    companion object {
        const val pluginPackagePrefix = "org.fcitx.fcitx5.android.plugin."
        const val pluginPackageSuffix = ".${BuildConfig.BUILD_TYPE}"
        const val pluginAPI = "0.1"

        /**
         * Components an interactive input panel plugin must provide in its UI:
         * - `layout_switch`: the "?123" key, calling [org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanelHost.switchToSymbolLayout]
         * - `input_method_switch`: the language key, calling
         *   [org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanelHost.switchInputMethod]
         *   (and [org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanelHost.showInputMethodPicker]
         *   on long press)
         */
        val requiredPanelComponents = setOf("layout_switch", "input_method_switch")
    }
}