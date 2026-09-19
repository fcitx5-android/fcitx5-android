/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.main.MainViewModel.ButtonMode
import org.fcitx.fcitx5.android.utils.item
import splitties.resources.styledColor

class EditDeleteMenuProvider<T>(
    private val buttonMode: LiveData<ButtonMode>,
    private val editButtonAction: (() -> Unit)?,
    private val deleteButtonAction: (() -> Unit)?,
    menuHost: T,
    lifecycleOwner: LifecycleOwner,
) : MenuProvider where T : MenuHost, T : Context {

    private val tint = menuHost.styledColor(android.R.attr.colorControlNormal)

    init {
        buttonMode.observe(lifecycleOwner) { menuHost.invalidateMenu() }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        when (buttonMode.value) {
            ButtonMode.EDIT -> {
                if (editButtonAction != null) {
                    menu.item(R.string.edit, R.drawable.ic_baseline_edit_24, tint, true) {
                        editButtonAction()
                    }
                }
            }
            ButtonMode.DELETE -> {
                if (deleteButtonAction != null) {
                    menu.item(R.string.delete, R.drawable.ic_baseline_delete_24, tint, true) {
                        deleteButtonAction()
                    }
                }
            }
            ButtonMode.NONE, null -> { /* no buttons */ }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
}