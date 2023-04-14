package org.fcitx.fcitx5.android.core.data

sealed interface PluginLoadFailed {
    data class PathConflict(
        val plugin: PluginDescriptor,
        val path: String,
        val existingSrc: FileSource
    ) : PluginLoadFailed

    object MissingPluginDescriptor : PluginLoadFailed {
        override fun toString(): String = "MissingPluginDescriptor"
    }

    object PluginDescriptorParseError : PluginLoadFailed {
        override fun toString(): String = "MissingPluginDescriptor"
    }

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