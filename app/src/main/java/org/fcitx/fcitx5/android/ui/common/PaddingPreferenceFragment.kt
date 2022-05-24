package org.fcitx.fcitx5.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.preference.PreferenceFragmentCompat
import org.fcitx.fcitx5.android.utils.applyNavBarInsetsBottomPadding

abstract class PaddingPreferenceFragment : PreferenceFragmentCompat() {

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).apply {
        listView.applyNavBarInsetsBottomPadding()
    }
}