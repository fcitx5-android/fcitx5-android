package me.rocka.fcitx5test.ui.main

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.service.FcitxDaemonManager
import me.rocka.fcitx5test.utils.appContext

class MainViewModel : ViewModel() {

    val isFcitxReady
        get() = this::fcitx.isInitialized && fcitx.lifecycle.currentState == Lifecycle.State.STARTED

    val toolbarTitle = MutableLiveData<String>()

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    // don't block initialization
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

    init {
        FcitxDaemonManager.instance.bindFcitxDaemonAsync(appContext, javaClass.name) {
            fcitx = getFcitxInstance()
        }
    }

    override fun onCleared() {
        FcitxDaemonManager.instance.unbind(appContext, javaClass.name)
    }
}