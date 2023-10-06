package org.fcitx.fcitx5.android.utils

import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.IOException

object FileUtil {

    /**
     * Delete a [File].
     * If it's a directory, delete its contents first.
     * If it's a symlink, don't follow.
     */
    fun removeFile(file: File) = runCatching {
        if (!file.exists())
            return Result.success(Unit)
        val isSymlink = OsConstants.S_ISLNK(Os.lstat(file.path).st_mode)
        // deleteRecursively follows symlink, so we want to make sure it's not a symlink
        val result = if (!isSymlink)
            file.deleteRecursively()
        else
            file.delete()
        if (!result)
            throw IOException("Cannot delete '${file.path}'")
    }

    fun symlink(source: File, target: File) = runCatching {
        target.parentFile?.mkdirs()
        Os.symlink(source.path, target.path)
    }
}