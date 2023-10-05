package org.fcitx.fcitx5.android.utils

import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.IOException

object FileUtil {

    /**
     * Delete a path
     * If it's a directory, delete its contents first
     * If it's a symlink, don't follow
     */
    fun removePath(path: String) = runCatching {
        val file = File(path)
        if (!file.exists())
            return Result.success(Unit)
        val isSymlink = OsConstants.S_ISLNK(Os.lstat(path).st_mode)
        // deleteRecursively follows symlink, so we want to make sure it's not a symlink
        val result = if (!isSymlink)
            file.deleteRecursively()
        else
            file.delete()
        if (!result)
            throw IOException("$path can not be deleted")
    }

    fun symlink(source: String, target: String) = runCatching {
        Os.symlink(source, target)
    }
}