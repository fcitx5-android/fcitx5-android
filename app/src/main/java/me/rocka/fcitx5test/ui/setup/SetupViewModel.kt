package me.rocka.fcitx5test.ui.setup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SetupViewModel : ViewModel() {
    val isAllDone = MutableLiveData(false)
}