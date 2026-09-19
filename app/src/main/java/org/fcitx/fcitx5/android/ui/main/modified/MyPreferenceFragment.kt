/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class MyPreferenceFragment : PreferenceFragmentCompat() {
    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (parentFragmentManager.findFragmentByTag(javaClass.name) != null) return
        val f = when (preference) {
            is EditTextPreference -> MyEditTextPreferenceDialogFragment.newInstance(preference.key)
            is ListPreference -> MyListPreferenceDialogFragment.newInstance(preference.key)
            else -> return super.onDisplayPreferenceDialog(preference)
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, javaClass.name)
    }
}