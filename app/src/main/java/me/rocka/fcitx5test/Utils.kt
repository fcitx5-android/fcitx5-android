package me.rocka.fcitx5test

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.copyFileOrDir(path: String): Unit = runCatching {
    with(assets) {
        val list = list(path) ?: throw IOException("No asset $path")
        if (list.isEmpty()) {
            copyFile(path)
        } else {
            val dir = File("${applicationInfo.dataDir}/${path}")
            if (!dir.exists()) dir.mkdir()
            list.forEach {
                copyFileOrDir("${path}/$it")
            }
        }

    }
}.getOrThrow()


private fun Context.copyFile(filename: String) = runCatching {
    with(assets) {
        open(filename).copyTo(
            FileOutputStream("${applicationInfo.dataDir}/${filename}")
        )
    }
    Unit
}.getOrThrow()
