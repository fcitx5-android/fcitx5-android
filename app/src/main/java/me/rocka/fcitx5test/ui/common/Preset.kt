package me.rocka.fcitx5test.ui.common

import android.content.Context
import android.widget.CheckBox
import android.widget.ImageButton

class CheckBoxListUi<T>(
    ctx: Context,
    initialEntries: List<T>,
    initCheckBox: (CheckBox.(Int) -> Unit),
    initSettingsButton: (ImageButton.(Int) -> Unit),
    val show: (T) -> String
) : BaseDynamicListUi<T>(
    ctx,
    Mode.Immutable(),
    initialEntries,
    false,
    initCheckBox,
    initSettingsButton
) {
    override fun showEntry(x: T): String = show(x)
}
