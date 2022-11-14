package org.fcitx.fcitx5.android.input.editorinfo

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow

class EditorInfoWindow : InputWindow.ExtendedInputWindow<EditorInfoWindow>(),
    InputBroadcastReceiver {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val theme by manager.theme()

    private val ui by lazy {
        EditorInfoUi(context, theme)
    }

    override val title by lazy {
        context.getString(R.string.editor_info_inspector)
    }

    override fun onCreateView() = ui.root

    override fun onAttached() {
        onEditorInfoUpdate(service.editorInfo)
    }

    override fun onEditorInfoUpdate(info: EditorInfo?) {
        if (info == null) return
        ui.setValues(EditorInfoParser.parse(info))
    }

    override fun onDetached() {}
}
