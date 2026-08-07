/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.graphics.Typeface
import android.widget.HorizontalScrollView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.utils.alpha
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter

class ClipboardCategoryBarUi(override val ctx: Context, private val theme: Theme) : Ui {
    private data class CategorySpec(
        val category: ClipboardCategory?,
        val textRes: Int
    )

    private val keyRipple by ThemeManager.prefs.keyRippleEffect

    private val specs = listOf(
        CategorySpec(null, R.string.clipboard_category_all),
        CategorySpec(ClipboardCategory.Phone, R.string.clipboard_category_phone),
        CategorySpec(ClipboardCategory.Email, R.string.clipboard_category_email),
        CategorySpec(ClipboardCategory.Url, R.string.clipboard_category_url),
        CategorySpec(ClipboardCategory.Otp, R.string.clipboard_category_otp),
        CategorySpec(ClipboardCategory.TrackingToken, R.string.clipboard_category_tracking_token),
        CategorySpec(ClipboardCategory.Other, R.string.clipboard_category_other)
    )

    private val tabs = specs.associate { spec ->
        spec.category to textView {
            setText(spec.textRes)
            textSize = 14f
            gravity = gravityCenter
            isClickable = true
            contentDescription = text
            if (keyRipple) {
                background = rippleDrawable(theme.keyPressHighlightColor)
            } else {
                foreground = pressHighlightDrawable(theme.keyPressHighlightColor)
            }
            setOnClickListener { onCategorySelected?.invoke(spec.category) }
        }
    }

    private val content = horizontalLayout {
        specs.forEach { spec ->
            add(tabs.getValue(spec.category), lParams(dp(68), matchParent))
        }
    }

    override val root = view(::HorizontalScrollView) {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        addView(content, lParams(wrapContent, matchParent))
    }

    private var onCategorySelected: ((ClipboardCategory?) -> Unit)? = null

    init {
        setActiveCategory(null)
    }

    fun setOnCategorySelectedListener(listener: (ClipboardCategory?) -> Unit) {
        onCategorySelected = listener
    }

    fun setActiveCategory(category: ClipboardCategory?) {
        tabs.forEach { (tabCategory, tab) ->
            val active = tabCategory == category
            tab.setTextColor(theme.keyTextColor.alpha(if (active) 1f else 0.5f))
            tab.typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        if (category == null) root.smoothScrollTo(0, 0)
    }
}
