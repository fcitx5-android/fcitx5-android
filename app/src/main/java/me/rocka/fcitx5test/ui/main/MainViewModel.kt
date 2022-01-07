package me.rocka.fcitx5test.ui.main

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.rocka.fcitx5test.FcitxApplication
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.service.FcitxDaemonManager

class MainViewModel : ViewModel() {

    private val context: Context
        get() = FcitxApplication.getInstance().applicationContext

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
        FcitxDaemonManager.instance.bindFcitxDaemonAsync(context, javaClass.name) {
            fcitx = getFcitxInstance()
        }
    }

    override fun onCleared() {
        FcitxDaemonManager.instance.unbind(context, javaClass.name)
    }
}