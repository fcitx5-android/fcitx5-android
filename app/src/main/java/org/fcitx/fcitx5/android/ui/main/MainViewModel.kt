/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.utils.appContext

class MainViewModel : ViewModel() {

    val toolbarTitle = MutableLiveData(appContext.getString(R.string.app_name))

    val toolbarShadow = MutableLiveData(true)

    val toolbarSaveButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val toolbarEditButtonVisible = MutableLiveData(false)

    val toolbarEditButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val toolbarDeleteButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val aboutButton = MutableLiveData(false)

    val fcitx: FcitxConnection = FcitxDaemon.connect(javaClass.name)

    fun setToolbarTitle(title: String) {
        toolbarTitle.value = title
    }

    fun enableToolbarShadow() {
        toolbarShadow.value = true
    }

    fun disableToolbarShadow() {
        toolbarShadow.value = false
    }

    fun enableToolbarSaveButton(onClick: () -> Unit) {
        toolbarSaveButtonOnClickListener.value = onClick
    }

    fun disableToolbarSaveButton() {
        toolbarSaveButtonOnClickListener.value = null
    }

    fun enableToolbarEditButton(visible: Boolean = true, onClick: () -> Unit) {
        toolbarEditButtonOnClickListener.value = onClick
        toolbarEditButtonVisible.value = visible
    }

    fun disableToolbarEditButton() {
        toolbarEditButtonOnClickListener.value = null
        hideToolbarEditButton()
    }

    fun hideToolbarEditButton() {
        toolbarEditButtonVisible.value = false
    }

    fun showToolbarEditButton() {
        toolbarEditButtonVisible.value = true
    }

    fun enableToolbarDeleteButton(onClick: () -> Unit) {
        toolbarDeleteButtonOnClickListener.value = onClick
    }

    fun disableToolbarDeleteButton() {
        toolbarDeleteButtonOnClickListener.value = null
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