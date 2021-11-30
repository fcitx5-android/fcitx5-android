package me.rocka.fcitx5test

import android.content.*
import android.inputmethodservice.InputMethodService
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import androidx.core.view.children
import androidx.preference.PreferenceManager
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
        open(filename).use { i ->
            FileOutputStream("${applicationInfo.dataDir}/${filename}").use { o ->
                i.copyTo(o)
                o.close()
            }
        }
    }
}.getOrThrow()

fun View.allChildren(): List<View> {
    if (this !is ViewGroup)
        return listOf(this)
    val result = mutableListOf<View>()
    children.forEach { result.addAll(it.allChildren()) }
    return result.toList()
}

fun Context.bindFcitxDaemon(
    onDisconnected: () -> Unit = {},
    onConnected: FcitxDaemon.FcitxBinder.() -> Unit
): ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        onConnected(service as FcitxDaemon.FcitxBinder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected()
    }

}.also {
    bindService(
        Intent(this, FcitxDaemon::class.java),
        it, Context.BIND_AUTO_CREATE
    )
}

interface MyOnClickListener : View.OnClickListener, View.OnLongClickListener {
    fun View.setOnClickListenerWithMe(block: (View) -> Unit) {
        setOnClickListener {
            this@MyOnClickListener.onClick(it)
            block(it)
        }
    }

    fun View.setOnLongClickListenerWithMe(block: (View) -> Boolean) {
        setOnLongClickListener {
            this@MyOnClickListener.onLongClick(it) and block(it)
        }
    }
}

fun Context.registerSharedPerfChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener,
    vararg keys: String
) {
    PreferenceManager.getDefaultSharedPreferences(this).let { sp ->
        sp.registerOnSharedPreferenceChangeListener(listener)
        keys.forEach { listener.onSharedPreferenceChanged(sp, it) }
    }
}

fun Context.unregisterSharedPerfChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener
) {
    PreferenceManager
        .getDefaultSharedPreferences(this)
        .unregisterOnSharedPreferenceChangeListener(listener)
}

val InputMethodService.inputConnection: InputConnection?
    get() = currentInputConnection
