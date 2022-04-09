package org.fcitx.fcitx5.android.utils

import android.content.ContentResolver
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.sun.jna.Library
import com.sun.jna.Native
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import splitties.experimental.InternalSplittiesApi
import splitties.resources.withResolvedThemeAttribute

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
