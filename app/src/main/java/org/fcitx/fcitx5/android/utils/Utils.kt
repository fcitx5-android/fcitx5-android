package org.fcitx.fcitx5.android.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.sun.jna.Library
import com.sun.jna.Native
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import splitties.experimental.InternalSplittiesApi
import splitties.resources.withResolvedThemeAttribute
import java.text.SimpleDateFormat
import java.util.*


val InputMethodService.inputConnection: InputConnection?
    get() = currentInputConnection

fun ViewPager2.getCurrentFragment(fragmentManager: FragmentManager): Fragment? =
    fragmentManager.findFragmentByTag("f$currentItem")

val appContext: Context
    get() = FcitxApplication.getInstance().applicationContext

fun Uri.queryFileName(contentResolver: ContentResolver) =
    contentResolver.query(
        this,
        null, null, null, null
    )?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(index)
    }

inline fun <reified T : Library> nativeLib(name: String): Lazy<T> = lazy {
    Native.load(name, T::class.java)
}

fun View.globalLayoutListener(repeat: () -> Boolean = { true }, block: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (!repeat())
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            block()
        }
    })
}

fun View.oneShotGlobalLayoutListener(block: () -> Unit) = globalLayoutListener({ false }, block)

fun <T : RecyclerView.ViewHolder> RecyclerView.Adapter<T>.onDataChanged(block: () -> Unit) =
    registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            block()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            block()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            block()
        }
    })

fun View.hapticIfEnabled() {
    if (AppPrefs.getInstance().keyboard.buttonHapticFeedback.getValue())
        performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
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
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
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

// only portrait
fun Context.keyboardWindowAspectRatio(): Pair<Int, Int> {
    val x: Int
    val y: Int
    when (resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            x = resources.displayMetrics.heightPixels
            y = resources.displayMetrics.widthPixels
        }
        else -> {
            x = resources.displayMetrics.widthPixels
            y = resources.displayMetrics.heightPixels
        }

    }
    return x to y *
            AppPrefs.getInstance().keyboard.keyboardHeightPercent.getValue() / 100
}