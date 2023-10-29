/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import splitties.experimental.InternalSplittiesApi
import splitties.resources.withResolvedThemeAttribute
import splitties.views.bottomPadding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.WeakHashMap
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

fun ViewPager2.getCurrentFragment(fragmentManager: FragmentManager): Fragment? =
    fragmentManager.findFragmentByTag("f$currentItem")

val appContext: Context
    get() = FcitxApplication.getInstance().applicationContext

fun Context.toast(string: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, string, duration).show()
}

fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    toast(getString(resId), duration)
}

fun ContentResolver.queryFileName(uri: Uri) =
    query(uri, null, null, null, null)?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(index)
    }

val EditText.str: String get() = editableText.toString()

@OptIn(InternalSplittiesApi::class)
fun Context.styledFloat(@AttrRes attrRes: Int) = withResolvedThemeAttribute(attrRes) {
    when (type) {
        TypedValue.TYPE_FLOAT -> float
        else -> throw IllegalArgumentException("float attribute expected")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun View.styledFloat(@AttrRes attrRes: Int) = context.styledFloat(attrRes)

@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.styledFloat(@AttrRes attrRes: Int) = context!!.styledFloat(attrRes)

fun isUiThread() = Looper.getMainLooper().isCurrentThread

fun formatDateTime(timeMillis: Long? = null): String =
    SimpleDateFormat.getDateTimeInstance().format(timeMillis?.let { Date(it) } ?: Date())

private val iso8601DateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

fun iso8601UTCDateTime(timeMillis: Long? = null): String =
    iso8601DateFormat.format(timeMillis?.let { Date(it) } ?: Date())

fun NavController.navigateFromMain(@IdRes dest: Int, bundle: Bundle? = null) {
    popBackStack(R.id.mainFragment, false)
    navigate(dest, bundle)
}

fun darkenColorFilter(percent: Int): ColorFilter {
    val value = percent * 255 / 100
    return PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
}

@Suppress("unused")
inline val ConstraintLayout.LayoutParams.unset
    get() = ConstraintLayout.LayoutParams.UNSET

@Suppress("NOTHING_TO_INLINE")
inline fun <T, U> kotlin.reflect.KFunction1<T, U>.upcast(): (T) -> U = this

@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.identity() = arrow.core.identity(this)

fun Configuration.isDarkMode() =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun Activity.applyTranslucentSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    // windowLightNavigationBar is available for 27+
    window.navigationBarColor =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            Color.TRANSPARENT
        } else {
            // com.android.internal.R.color.system_bar_background_semi_transparent
            0x66000000
        }
}

fun RecyclerView.applyNavBarInsetsBottomPadding() {
    clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).also {
            bottomPadding = it.bottom
        }
        windowInsets
    }
}

suspend fun <T> Result<T>.toast(context: Context) = withContext(Dispatchers.Main.immediate) {
    onSuccess {
        Toast.makeText(context, R.string.done, Toast.LENGTH_SHORT).show()
    }
    onFailure {
        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
    }
}

suspend fun errorDialog(context: Context, title: String, message: String) {
    withContext(Dispatchers.Main.immediate) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}

fun Int.alpha(a: Float) = ColorUtils.setAlphaComponent(this, (a * 0xff).roundToInt())

fun SeekBar.setOnChangeListener(listener: SeekBar.(progress: Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            listener.invoke(seekBar, progress)
        }
    })
}

@SuppressLint("PrivateApi")
fun getSystemProperty(key: String): String {
    return try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as String
    } catch (e: Exception) {
        ""
    }
}

fun isSystemSettingEnabled(key: String): Boolean {
    return try {
        Settings.System.getInt(appContext.contentResolver, key) == 1
    } catch (e: Exception) {
        false
    }
}

/**
 * @return top-level files in zip file
 */
fun ZipInputStream.extract(destDir: File): List<File> {
    var entry = nextEntry
    val canonicalDest = destDir.canonicalPath
    while (entry != null) {
        if (!entry.isDirectory) {
            val file = File(destDir, entry.name)
            if (!file.canonicalPath.startsWith(canonicalDest))
                throw SecurityException()
            copyTo(file.outputStream())
        } else {
            val dir = File(destDir, entry.name)
            dir.mkdir()
        }
        entry = nextEntry
    }
    return destDir.listFiles()?.toList() ?: emptyList()
}

inline fun <T> withTempDir(block: (File) -> T): T {
    val dir = appContext.cacheDir.resolve(System.currentTimeMillis().toString()).also {
        it.mkdirs()
    }
    try {
        return block(dir)
    } finally {
        dir.deleteRecursively()
    }
}

@Suppress("FunctionName")
fun <T> WeakHashSet(): MutableSet<T> = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

val javaIdRegex = Regex("(?:\\b[_a-zA-Z]|\\B\\$)\\w*+")

fun InputConnection.withBatchEdit(block: InputConnection.() -> Unit) {
    beginBatchEdit()
    block.invoke(this)
    endBatchEdit()
}
