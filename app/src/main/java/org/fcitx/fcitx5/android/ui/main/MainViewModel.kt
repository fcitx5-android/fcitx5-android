package org.fcitx.fcitx5.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.utils.appContext

class MainViewModel : ViewModel() {

    val toolbarTitle = MutableLiveData(appContext.getString(R.string.app_name))

    val appbarShadow = MutableLiveData(true)

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val aboutButton = MutableLiveData<Boolean>()

    val fcitx: FcitxConnection = FcitxDaemon.connect(javaClass.name)

    fun setToolbarTitle(title: String) {
        toolbarTitle.value = title
    }

    fun enableAppbarShadow() {
        appbarShadow.value = true
    }

    fun disableAppbarShadow() {
        appbarShadow.value = false
    }

    fun enableToolbarSaveButton(onClick: () -> Unit) {
        toolbarSaveButtonOnClickListener.value = onClick
    }

    fun disableToolbarSaveButton() {
        toolbarSaveButtonOnClickListener.value = null
    }

    fun enableAboutButton() {
        aboutButton.value = true
    }

    fun disableAboutButton() {
        aboutButton.value = false
    }

    override fun onCleared() {
        FcitxDaemon.disconnect(javaClass.name)
    }
}