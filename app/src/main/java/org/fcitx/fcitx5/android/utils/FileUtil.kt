package org.fcitx.fcitx5.android.utils

import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.IOException

object FileUtil {

    private fun File.isSymlink(): Boolean = OsConstants.S_ISLNK(Os.lstat(path).st_mode)

    /**
     * Delete a [File].
     * If it's a directory, delete its contents first.
     * If it's a symlink, don't follow.
     */
    fun removeFile(file: File) = runCatching {
        if (!file.exists())
            return Result.success(Unit)
        val result = if (file.isSymlink()) {
            file.delete()
        } else if (file.isDirectory) {
            file.walkBottomUp()
                .onEnter {
                    // delete symlink (to directory) instead of entering it
                    if (it.isSymlink()) {
                        it.delete()
                        false
                    } else {
                        true
                    }
                }
                .fold(true) { acc, it ->
                    if (!it.exists()) acc else it.delete()
                }
        } else {
            file.delete()
        }
        if (!result)
            throw IOException("Cannot delete '${file.path}'")
    }

    fun symlink(source: File, target: File) = runCatching {
        target.parentFile?.mkdirs()
        Os.symlink(source.path, target.path)
    }
}