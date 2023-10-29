/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.editorinfo

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow

class EditorInfoWindow : InputWindow.ExtendedInputWindow<EditorInfoWindow>() {

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
        ui.setValues(EditorInfoParser.parse(service.currentInputEditorInfo))
    }

    override fun onDetached() {}
}
