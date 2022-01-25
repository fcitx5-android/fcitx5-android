package me.rocka.fcitx5test.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.service.FcitxDaemonManager
import timber.log.Timber

class MainViewModel : ViewModel() {

    val isFcitxReady
        get() = this::fcitx.isInitialized && fcitx.isReady

    val toolbarTitle = MutableLiveData<String>()

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

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