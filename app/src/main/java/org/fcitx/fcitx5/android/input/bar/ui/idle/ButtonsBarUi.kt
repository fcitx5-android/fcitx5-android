/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.content.Context
import androidx.annotation.DrawableRes
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.view

class ButtonsBarUi(override val ctx: Context, private val theme: Theme) : Ui {

    override val root = view(::FlexboxLayout) {
        alignItems = AlignItems.CENTER
        justifyContent = JustifyContent.SPACE_AROUND
    }

    private fun toolButton(@DrawableRes icon: Int) = ToolButton(ctx, icon, theme).also {
        val size = ctx.dp(40)
        root.addView(it, FlexboxLayout.LayoutParams(size, size))
    }

    val undoButton = toolButton(R.drawable.ic_baseline_undo_24)

    val redoButton = toolButton(R.drawable.ic_baseline_redo_24)

    val cursorMoveButton = toolButton(R.drawable.ic_cursor_move)

    val clipboardButton = toolButton(R.drawable.ic_clipboard)

    val moreButton = toolButton(R.drawable.ic_baseline_more_horiz_24)

}
