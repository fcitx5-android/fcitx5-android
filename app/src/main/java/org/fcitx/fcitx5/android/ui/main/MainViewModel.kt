package org.fcitx.fcitx5.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.service.FcitxDaemonManager
import org.fcitx.fcitx5.android.utils.appContext

class MainViewModel : ViewModel() {

    val isFcitxReady
        get() = this::fcitx.isInitialized && fcitx.isReady

    val toolbarTitle = MutableLiveData(appContext.getString(R.string.app_name))

    val appbarShadow = MutableLiveData(true)

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val aboutButton = MutableLiveData<Boolean>()

    lateinit var fcitx: Fcitx

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

    val listeners = hashSetOf<MainViewModel.() -> Unit>()

    fun onBindFcitxInstance(block: MainViewModel.() -> Unit) {
        if (this::fcitx.isInitialized) block.invoke(this)
        else listeners.add(block)
    }

    init {
        FcitxDaemonManager.bindFcitxDaemon(javaClass.name) {
            fcitx = getFcitxDaemon().fcitx
            listeners.forEach { it.invoke(this@MainViewModel) }
            listeners.clear()
        }
    }

    override fun onCleared() {
        FcitxDaemonManager.unbind(javaClass.name)
    }
}