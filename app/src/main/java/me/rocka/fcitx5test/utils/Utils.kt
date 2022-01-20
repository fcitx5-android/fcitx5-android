package me.rocka.fcitx5test.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.viewpager2.widget.ViewPager2
import me.rocka.fcitx5test.FcitxApplication

fun View.allChildren(): List<View> {
    if (this !is ViewGroup)
        return listOf(this)
    val result = mutableListOf<View>()
    children.forEach { result.addAll(it.allChildren()) }
    return result.toList()
}


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

@SuppressLint("VisibleForTests")
// unrelated to Android components
// no need to be interacted on Android main thread
fun abusedLifecycleRegistry(owner: LifecycleOwner) = LifecycleRegistry.createUnsafe(owner)