/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import org.fcitx.fcitx5.android.ui.main.theme.SettingsPreferenceContent
import org.fcitx.fcitx5.android.ui.main.theme.SettingsTheme
import org.fcitx.fcitx5.android.ui.main.theme.SettingsThemeContent
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class MyPreferenceFragment : PreferenceFragmentCompat() {
    private var themedAdapter by mutableStateOf<PreferenceGroupAdapter?>(null)

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): androidx.recyclerview.widget.RecyclerView.Adapter<*> {
        return super.onCreateAdapter(preferenceScreen).also {
            themedAdapter = it as? PreferenceGroupAdapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val original = super.onCreateView(inflater, container, savedInstanceState)
        val theme = SettingsTheme.current()
        if (theme == SettingsTheme.MaterialDesign) return original
        return FrameLayout(requireContext()).apply {
            original.visibility = View.GONE
            addView(original, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SettingsThemeContent {
                        SettingsPreferenceContent(themedAdapter)
                    }
                }
            }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    override fun onDestroyView() {
        themedAdapter = null
        super.onDestroyView()
    }

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