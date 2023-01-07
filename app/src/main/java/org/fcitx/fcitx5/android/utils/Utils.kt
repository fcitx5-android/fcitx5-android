package org.fcitx.fcitx5.android.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import arrow.core.toOption
import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import splitties.experimental.InternalSplittiesApi
import splitties.resources.withResolvedThemeAttribute
import splitties.views.bottomPadding
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.math.roundToInt

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
    }.toOption()

inline fun <reified T : Library> nativeLib(name: String): Lazy<T> = lazy {
    Native.load(name, T::class.java)
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

@Suppress("unused")
inline val ConstraintLayout.LayoutParams.unset
    get() = ConstraintLayout.LayoutParams.UNSET

@Suppress("NOTHING_TO_INLINE")
inline fun <T, U> kotlin.reflect.KFunction1<T, U>.upcast(): (T) -> U = this

@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.identity() = arrow.core.identity(this)

fun Configuration.isDarkMode() =
    when (uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }

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

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, U> Result<T?>.bindOnNotNull(block: (T) -> Result<U>): Result<U>? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isSuccess && getOrThrow() != null -> block(getOrThrow()!!)
        isSuccess && getOrThrow() == null -> null
        else -> Result.failure(exceptionOrNull()!!)
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
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }
}

inline fun <reified T : Parcelable> Bundle.parcelableArray(key: String): Array<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArray(key, T::class.java)
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        getParcelableArray(key) as? Array<T>
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
    return Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
        .invoke(null, key) as String
}

fun ZipInputStream.extract(): List<File> {
    val tempDir = File(createTempDirectory().pathString)
    val extracted = mutableListOf<File>()
    var entry = nextEntry
    while (entry != null && !entry.isDirectory) {
        val file = File(tempDir, entry.name)
        copyTo(file.outputStream())
        extracted.add(file)
        entry = nextEntry
    }
    return extracted
}

fun Context.getHostActivity(): AppCompatActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is AppCompatActivity)
            return context
        else
            context = context.baseContext
    }
    return null
}