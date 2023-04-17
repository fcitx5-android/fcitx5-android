package org.fcitx.fcitx5.android.ui.main.settings.behavior

import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment

class KeyboardSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().keyboard)
