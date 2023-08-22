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