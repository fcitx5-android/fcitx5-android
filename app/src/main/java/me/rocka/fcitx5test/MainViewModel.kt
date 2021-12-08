package me.rocka.fcitx5test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.rocka.fcitx5test.native.Fcitx

class MainViewModel : ViewModel() {
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

}