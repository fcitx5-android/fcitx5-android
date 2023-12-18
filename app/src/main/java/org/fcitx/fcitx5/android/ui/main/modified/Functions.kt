/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.SwitchPreference
import splitties.dimensions.dp
import splitties.views.dsl.core.verticalMargin

private val mDefault by lazy {
    Preference::class.java
        .getDeclaredField("mDefaultValue")
        .apply { isAccessible = true }
}

private fun <T : Preference> T.def() =
    mDefault.get(this)

fun <T : EditTextPreference> T.restore() {
    // must `callChangeListener` before `setText`
    // https://android.googlesource.com/platform/frameworks/support/+/872b66efac82f0b0a3fac4bb14a789464ab19f96/preference/preference/src/main/java/androidx/preference/EditTextPreferenceDialogFragmentCompat.java#146
    (def() as? String)?.let {
        if (callChangeListener(it)) {
            text = it
        }
    }
}

fun <T : ListPreference> T.restore() {
    (def() as? String)?.let {
        if (callChangeListener(it)) {
            value = it
        }
    }
}

fun <T : SwitchPreference> T.restore() {
    (def() as? Boolean)?.let {
        if (callChangeListener(it)) {
            isChecked = it
        }
    }
}

fun PreferenceDialogFragmentCompat.fixDialogMargin(contentView: View) {
    // dialogMessage text in AlertDialog has 48dp bottom margin, just make it smaller
    contentView.findViewById<View>(android.R.id.message)?.updateLayoutParams<MarginLayoutParams> {
        verticalMargin = requireContext().dp(8)
    }
}
