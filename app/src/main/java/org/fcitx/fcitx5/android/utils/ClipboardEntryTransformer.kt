package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer

private const val FALLBACK_DESC = "<no description>"

val IClipboardEntryTransformer.desc
    get() = runCatching { description }.getOrElse { FALLBACK_DESC }

fun IClipboardEntryTransformer.descEquals(other: IClipboardEntryTransformer): Boolean {
    return try {
        description!! == other.description!!
    } catch (e: Exception) {
        false
    }
}
