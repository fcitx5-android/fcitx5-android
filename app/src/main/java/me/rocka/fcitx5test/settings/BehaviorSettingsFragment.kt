package me.rocka.fcitx5test.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.rocka.fcitx5test.R

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }
}
