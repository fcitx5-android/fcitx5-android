package me.rocka.fcitx5test.utils

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2

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
