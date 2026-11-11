/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.text.TextUtils
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityCenter
import splitties.views.setPaddingDp

class TokenizedClipboardUi(
    override val ctx: Context,
    private val theme: Theme
) : Ui {

    private val keyBorder by ThemeManager.prefs.keyBorder

    val backButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_back_24, theme)

    val copyButton = createActionButton(R.string.copy)
    val selectAllButton = createActionButton(R.string.tokenized_clipboard_select_all)
    val invertSelectionButton = createActionButton(R.string.tokenized_clipboard_invert_selection)
    val clearSelectionButton = createActionButton(R.string.tokenized_clipboard_clear_selection)
    val sendButton = createActionButton(R.string.tokenized_clipboard_send)

    val recyclerView = recyclerView {
        overScrollMode = View.OVER_SCROLL_NEVER
        clipToPadding = false
        setPaddingDp(12, 12, 12, 12)
    }

    val emptyView = textView {
        text = ctx.getString(R.string.tokenized_clipboard_empty_state)
        gravity = gravityCenter
        textSize = 15f
        setTextColor(theme.altKeyTextColor)
        isVisible = false
    }

    private val actionBar = horizontalLayout {
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        add(copyButton, lParams(wrapContent, dp(28)))
        add(selectAllButton, lParams(wrapContent, dp(28)))
        add(invertSelectionButton, lParams(wrapContent, dp(28)))
        add(clearSelectionButton, lParams(wrapContent, dp(28)))
        add(sendButton, lParams(wrapContent, dp(28)))
    }

    private val topBar = constraintLayout {
        add(backButton, lParams(dp(40), dp(40)) {
            startOfParent()
            topOfParent()
        })
        add(actionBar, lParams(wrapContent, wrapContent) {
            endOfParent()
            centerVertically()
        })
        add(textView {
            text = ctx.getString(R.string.tokenized_clipboard)
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            textSize = 16f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTextColor(theme.altKeyTextColor)
            isSingleLine = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
        }, lParams(0, dp(40)) {
            after(backButton, dp(8))
            before(actionBar, dp(8))
            centerVertically()
        })
    }

    override val root = verticalLayout {
        if (!keyBorder) {
            backgroundColor = theme.barColor
        }
        add(topBar, lParams(matchParent, wrapContent))
        add(frameLayout {
            add(recyclerView, FrameLayout.LayoutParams(matchParent, matchParent))
            add(emptyView, FrameLayout.LayoutParams(matchParent, matchParent))
        }, lParams(matchParent, 0) {
            weight = 1f
        })
    }

    fun updateSelectionState(selectedCount: Int, totalCount: Int) {
        val hasSelection = selectedCount > 0
        copyButton.isEnabled = hasSelection
        sendButton.isEnabled = hasSelection
        clearSelectionButton.isEnabled = hasSelection
        copyButton.alpha = if (hasSelection) 1f else 0.5f
        sendButton.alpha = if (hasSelection) 1f else 0.5f
        clearSelectionButton.alpha = if (hasSelection) 1f else 0.5f
        selectAllButton.isEnabled = totalCount > 0
        invertSelectionButton.isEnabled = totalCount > 0
        selectAllButton.alpha = if (totalCount > 0) 1f else 0.5f
        invertSelectionButton.alpha = if (totalCount > 0) 1f else 0.5f
        selectAllButton.text = ctx.getString(
            if (totalCount > 0 && selectedCount == totalCount) {
                R.string.tokenized_clipboard_unselect_all
            } else {
                R.string.tokenized_clipboard_select_all
            }
        )
    }

    fun setEmptyState(show: Boolean) {
        emptyView.isVisible = show
        recyclerView.isVisible = !show
    }

    private fun createActionButton(textRes: Int) = textView {
        text = ctx.getString(textRes)
        gravity = gravityCenter
        minWidth = dp(40)
        textSize = 13f
        setPaddingDp(6, 0, 6, 0)
        background = pressHighlightDrawable(theme.keyPressHighlightColor)
        setTextColor(theme.altKeyTextColor)
    }
}
