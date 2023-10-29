/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import android.annotation.SuppressLint
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class MyPreferenceFragment : PreferenceFragmentCompat() {
    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun onDisplayPreferenceDialog(preference: Preference) {

        var handled = false
        if (callbackFragment is OnPreferenceDisplayDialogCallback) {
            handled =
                (callbackFragment as OnPreferenceDisplayDialogCallback).onPreferenceDisplayDialog(
                    this,
                    preference
                )
        }

        var callbackFragment: Fragment? = this
        while (!handled && callbackFragment != null) {
            if (callbackFragment is OnPreferenceDisplayDialogCallback) {
                handled = (callbackFragment as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
            }
            callbackFragment = callbackFragment.parentFragment
        }
        if (!handled && context is OnPreferenceDisplayDialogCallback) {
            handled = (context as OnPreferenceDisplayDialogCallback)
                .onPreferenceDisplayDialog(this, preference)
        }
        if (!handled && activity is OnPreferenceDisplayDialogCallback) {
            handled = (activity as OnPreferenceDisplayDialogCallback)
                .onPreferenceDisplayDialog(this, preference)
        }

        if (handled) {
            return
        }

        if (parentFragmentManager.findFragmentByTag(javaClass.name) != null) {
            return
        }

        val f: DialogFragment = when (preference) {
            is EditTextPreference -> {
                MyEditTextPreferenceDialogFragment.newInstance(preference.getKey())
            }
            is ListPreference -> {
                MyListPreferenceDialogFragment.newInstance(preference.getKey())
            }
            else -> {
                throw IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: ${preference.javaClass.name}"
                )
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, javaClass.name)
    }
}