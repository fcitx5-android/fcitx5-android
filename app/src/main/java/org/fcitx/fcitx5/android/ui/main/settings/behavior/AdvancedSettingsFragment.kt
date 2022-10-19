package org.fcitx.fcitx5.android.ui.main.settings.behavior

import org.fcitx.fcitx5.android.data.prefs.AppPrefs

class AdvancedSettingsFragment: BehaviorSettingsFragment(AppPrefs.getInstance().advanced)
