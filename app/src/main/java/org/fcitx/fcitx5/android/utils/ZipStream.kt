/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import java.io.File
import java.util.zip.ZipInputStream

/**
 * @return top-level files in zip file
 */
fun ZipInputStream.extract(destDir: File): List<File> {
    var entry = nextEntry
    val canonicalDest = destDir.canonicalPath
    while (entry != null) {
        val dest = File(destDir, entry.name)
        if (!dest.canonicalPath.startsWith(canonicalDest)) throw SecurityException()
        if (entry.isDirectory) {
            dest.mkdirs()
        } else {
            val parent = dest.parentFile
            if (parent?.exists() == false) {
                parent.mkdirs()
            }
            copyTo(dest.outputStream())
        }
        entry = nextEntry
    }
    return destDir.listFiles()?.toList() ?: emptyList()
}
