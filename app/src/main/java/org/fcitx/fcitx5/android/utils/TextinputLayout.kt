/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent

inline fun Context.materialTextInput(
    initLayout: TextInputLayout.(TextInputEditText) -> Unit
): Pair<TextInputLayout, TextInputEditText> {
    val editText = view(::TextInputEditText)
    val inputLayout = view(::TextInputLayout) {
        add(editText, lParams(matchParent, wrapContent))
        initLayout.invoke(this, editText)
    }
    return inputLayout to editText
}

inline fun Ui.materialTextInput(
    initLayout: TextInputLayout.(TextInputEditText) -> Unit
) = ctx.materialTextInput(initLayout)
