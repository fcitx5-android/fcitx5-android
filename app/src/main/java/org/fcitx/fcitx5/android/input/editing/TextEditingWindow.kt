package org.fcitx.fcitx5.android.input.editing

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.wm.InputWindow

class TextEditingWindow : InputWindow.ExtendedInputWindow<TextEditingWindow>() {

    private val ui by lazy {
        TextEditingUi(context)
    }

    override val view by lazy {
        ui.root
    }

    override fun onAttached() {
    }

    override fun onDetached() {
    }

    override val title by lazy {
        context.getString(R.string.text_editing)
    }

    override val barExtension by lazy {
        ui.extension
    }
}
