package me.rocka.fcitx5test

import android.content.Context
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.view.ViewGroup
import androidx.core.view.children


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

fun View.allChildren(): List<View> {
    if (this !is ViewGroup)
        return listOf(this)
    val result = mutableListOf<View>()
    children.forEach { result.addAll(it.allChildren()) }
    return result.toList()
}
