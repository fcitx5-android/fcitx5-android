package org.fcitx.fcitx5.android.input.picker

fun symbolPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Symbol,
    data = PickerData.Symbol,
    density = PickerPageUi.Density.High
)

fun emojiPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Emoji,
    data = PickerData.Emoji,
    density = PickerPageUi.Density.Medium,
    popupPreview = false
)
