package org.fcitx.fcitx5.android.ui.main.modified

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference

private val mDefault by lazy {
    Preference::class
        .java
        .getDeclaredField("mDefaultValue")
        .apply { isAccessible = true }
}

private fun <T : Preference> T.def() =
    mDefault.get(this)

fun <T : EditTextPreference> T.restore() {
    def()?.let { text = it.toString() }
}

fun <T : ListPreference> T.restore() {
    def()?.let { it as? String }?.let { value = it }
}