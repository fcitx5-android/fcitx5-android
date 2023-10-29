/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.keyboard.ImageLayoutSwitchKey
import org.fcitx.fcitx5.android.input.keyboard.ImagePickerSwitchKey
import org.fcitx.fcitx5.android.input.keyboard.NumberKeyboard
import org.fcitx.fcitx5.android.input.keyboard.TextPickerSwitchKey

fun symbolPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Symbol,
    data = PickerData.Symbol,
    density = PickerPageUi.Density.High,
    switchKey = ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name)
)

fun emojiPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Emoji,
    data = PickerData.Emoji,
    density = PickerPageUi.Density.Medium,
    popupPreview = false,
    switchKey = TextPickerSwitchKey(":-)", PickerWindow.Key.Emoticon),
)

fun emoticonPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Emoticon,
    data = PickerData.Emoticon,
    density = PickerPageUi.Density.Low,
    popupPreview = false,
    switchKey = ImagePickerSwitchKey(R.drawable.ic_baseline_tag_faces_24, PickerWindow.Key.Emoji)
)
