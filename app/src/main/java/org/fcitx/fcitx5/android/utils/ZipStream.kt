/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
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
        if (!entry.isDirectory) {
            val file = File(destDir, entry.name)
            if (!file.canonicalPath.startsWith(canonicalDest)) throw SecurityException()
            copyTo(file.outputStream())
        } else {
            val dir = File(destDir, entry.name)
            dir.mkdir()
        }
        entry = nextEntry
    }
    return destDir.listFiles()?.toList() ?: emptyList()
}
