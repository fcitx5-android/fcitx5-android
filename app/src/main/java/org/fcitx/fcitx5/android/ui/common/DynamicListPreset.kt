/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton

@Suppress("FunctionName")
fun <T> Context.DynamicListUi(
    mode: BaseDynamicListUi.Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = false,
    initCheckBox: (CheckBox.(T) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE },
    show: (T) -> String
): BaseDynamicListUi<T> = object :
    BaseDynamicListUi<T>(
        this,
        mode,
        initialEntries,
        enableOrder,
        initCheckBox,
        initSettingsButton
    ) {
    init {
        addTouchCallback()
    }

    override fun showEntry(x: T): String = show(x)
}

@Suppress("FunctionName")
fun <T> Context.CheckBoxListUi(
    initialEntries: List<T>,
    initCheckBox: (CheckBox.(T) -> Unit),
    initSettingsButton: (ImageButton.(T) -> Unit),
    show: (T) -> String
) = DynamicListUi(
    BaseDynamicListUi.Mode.Immutable(),
    initialEntries,
    false,
    initCheckBox,
    initSettingsButton,
    show
)
