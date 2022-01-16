package me.rocka.fcitx5test.data

import android.util.Log
import me.rocka.fcitx5test.data.pinyin.Dictionary
import me.rocka.fcitx5test.data.pinyin.LibIMEDictionary
import me.rocka.fcitx5test.utils.appContext
import java.io.File
import java.io.IOException
import java.io.InputStream

object PinyinDictManager {
    init {
        System.loadLibrary("pinyindictionaryutils")
    }

    private val pinyinDicDir = File(
        appContext.getExternalFilesDir(null)!!, "data/pinyin/dictionaries"
    )

    private val nativeDir = File(appContext.applicationInfo.nativeLibraryDir)

    private val scel2org5 by lazy { File(nativeDir, scel2org5Name) }

    fun dictionaries(): List<Dictionary> = pinyinDicDir
        .listFiles()
        ?.mapNotNull { Dictionary.new(it) }
        ?.toList() ?: listOf()

    fun importFromFile(file: File) {
        val raw = Dictionary.new(file)
            ?: throw IllegalArgumentException("${file.path} is not a libime/text/sougou dictionary")
        // convert to libime format in dictionaries dir
        // preserve original file name
        val new = raw.toLibIMEDictionary(
            File(
                pinyinDicDir,
                file.nameWithoutExtension + LibIMEDictionary.EXT
            )
        )
        Log.i(javaClass.name, "Converted $raw to $new")
    }

    fun importFromInputStream(stream: InputStream, name: String) {
        val tempFile = File(appContext.cacheDir, name)
        tempFile.outputStream().use {
            stream.copyTo(it)
        }
        importFromFile(tempFile)
        tempFile.delete()
    }

    fun sougouDictConv(src: String, dest: String) {
        val process = Runtime.getRuntime()
            .exec(
                arrayOf(scel2org5.absolutePath, src),
                arrayOf("LD_LIBRARY_PATH=${nativeDir.absolutePath}")
            )
        process.waitFor()
        when (process.exitValue()) {
            0 -> File(dest).also { it.deleteOnExit() }.outputStream()
                .use { process.inputStream.copyTo(it) }
            else -> throw IOException(process.errorStream.bufferedReader().readText())
        }
    }

    @JvmStatic
    external fun pinyinDictConv(src: String, dest: String, mode: Boolean)

    const val MODE_BIN_TO_TXT = true
    const val MODE_TXT_TO_BIN = false
    private const val scel2org5Name = "libscel2org5.so"

}