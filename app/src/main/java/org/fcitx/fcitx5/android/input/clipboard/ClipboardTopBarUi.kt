/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.Typeface
import android.view.View
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.alpha
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.gravityCenter

class ClipboardTopBarUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val keyRipple by ThemeManager.prefs.keyRippleEffect

    inner class Tab(private val section: ClipboardPanelSection, textRes: Int) : Ui {
        override val ctx = this@ClipboardTopBarUi.ctx

        private val label = textView {
            setText(textRes)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = gravityCenter
        }

        override val root = view(::CustomGestureView) {
            isClickable = true
            contentDescription = label.text
            add(label, lParams(matchParent, matchParent))
            if (keyRipple) {
                background = rippleDrawable(theme.keyPressHighlightColor)
            } else {
                foreground = pressHighlightDrawable(theme.keyPressHighlightColor)
            }
            setOnClickListener { onSectionSelected?.invoke(section) }
        }

        fun setActive(active: Boolean) {
            label.setTextColor(theme.keyTextColor.alpha(if (active) 1f else 0.5f))
        }
    }

    private val clipboardTab = Tab(ClipboardPanelSection.Clipboard, R.string.clipboard)
    private val favoritesTab = Tab(ClipboardPanelSection.Favorites, R.string.favorites)

    val deleteAllButton = ToolButton(ctx, R.drawable.ic_baseline_delete_sweep_24, theme).apply {
        contentDescription = ctx.getString(R.string.delete_all)
    }

    private var onSectionSelected: ((ClipboardPanelSection) -> Unit)? = null

    override val root = constraintLayout {
        add(clipboardTab.root, lParams {
            topOfParent()
            startOfParent()
            bottomOfParent()
            before(favoritesTab.root)
        })
        add(favoritesTab.root, lParams {
            topOfParent()
            after(clipboardTab.root)
            bottomOfParent()
            before(deleteAllButton)
        })
        add(deleteAllButton, lParams(dp(40), dp(40)) {
            centerVertically()
            endOfParent()
        })
    }

    init {
        setActiveSection(ClipboardPanelSection.Clipboard)
    }

    fun setOnSectionSelectedListener(listener: (ClipboardPanelSection) -> Unit) {
        onSectionSelected = listener
    }

    fun setActiveSection(section: ClipboardPanelSection) {
        clipboardTab.setActive(section == ClipboardPanelSection.Clipboard)
        favoritesTab.setActive(section == ClipboardPanelSection.Favorites)
    }

    fun setDeleteButtonShown(shown: Boolean) {
        deleteAllButton.visibility = if (shown) View.VISIBLE else View.INVISIBLE
    }
}
