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

    enum class ButtonMode { NONE, EDIT, DELETE }

    val toolbarTitle = MutableLiveData(appContext.getString(R.string.app_name))

    val toolbarShadow = MutableLiveData(true)

    val toolbarButton = MutableLiveData(ButtonMode.NONE)

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

    override fun onCleared() {
        FcitxDaemon.disconnect(javaClass.name)
    }
}