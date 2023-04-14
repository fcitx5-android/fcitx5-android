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