package me.rocka.fcitx5test.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.rocka.fcitx5test.core.Fcitx
import me.rocka.fcitx5test.service.FcitxDaemonManager
import timber.log.Timber

class MainViewModel : ViewModel() {

    val isFcitxReady
        get() = this::fcitx.isInitialized && fcitx.isReady

    val toolbarTitle = MutableLiveData<String>()

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val aboutButton = MutableLiveData<Boolean>()

    lateinit var fcitx: Fcitx

    fun setToolbarTitle(title: String) {
        toolbarTitle.value = title
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

    init {
        Timber.d("init")
        FcitxDaemonManager.bindFcitxDaemon(javaClass.name) {
            fcitx = getFcitxDaemon().fcitx
        }
    }

    override fun onCleared() {
        Timber.d("onCleared")
        FcitxDaemonManager.unbind(javaClass.name)
    }
}