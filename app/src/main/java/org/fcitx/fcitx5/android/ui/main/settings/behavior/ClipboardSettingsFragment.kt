/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardSyncSettingsActivity
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment

class ClipboardSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().clipboard) {

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        val context = requireContext()
        screen.addPreference(
            Preference(context).apply {
                key = "clipboard_sync_settings_entry"
                title = context.getString(R.string.clipboard_sync_settings)
                summary = context.getString(R.string.clipboard_sync_settings_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    startActivity(Intent(context, ClipboardSyncSettingsActivity::class.java))
                    true
                }
            }
        )
    }
}
