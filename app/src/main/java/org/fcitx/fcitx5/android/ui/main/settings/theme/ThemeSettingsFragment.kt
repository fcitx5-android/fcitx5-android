package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.os.Build
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fcitx.fcitx5.android.data.theme.ThemeManager

class ThemeSettingsFragment : ManagedPreferenceFragment(ThemeManager.prefs) {
    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ThemeManager.syncToDeviceEncryptedStorage()
        }
        super.onStop()
    }
}
