/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.editorinfo

import android.content.Context
import android.widget.TableLayout
import android.widget.TableRow
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.dsl.core.*
import splitties.views.setPaddingDp

class EditorInfoUi(override val ctx: Context, private val theme: Theme) : Ui {

    private fun createTextView(str: String) = textView {
        text = str
        setPaddingDp(3)
        setTextColor(theme.keyTextColor)
    }

    private fun TableLayout.addRow(label: String, value: String) {
        addView(view(::TableRow) {
            addView(createTextView(label))
            addView(createTextView(value))
        })
    }

    val table = view(::TableLayout) {
        isStretchAllColumns = true
    }

    override val root = table
        .wrapInHorizontalScrollView()
        .wrapInScrollView { isFillViewport = true }

    fun setValues(values: Map<String, String>) {
        table.apply {
            removeAllViews()
            values.forEach { (k, v) ->
                addRow(k, v)
            }
        }
    }
}
