/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.imageDrawable
import kotlin.math.floor

class PopupMenuUi(
    override val ctx: Context,
    theme: Theme,
    bounds: Rect,
    onDismissSelf: PopupContainerUi.() -> Unit = {},
    private val items: Array<KeyDef.Popup.Menu.Item>
) : PopupContainerUi(ctx, theme, bounds, onDismissSelf) {

    private val keySize = ctx.dp(48)

    private val inactiveBackground = InsetDrawable(
        ShapeDrawable(OvalShape()).apply {
            paint.color = theme.accentKeyBackgroundColor
        },
        (keySize - ctx.dp(33)) / 2
    )

    private val activeBackground = InsetDrawable(
        ShapeDrawable(OvalShape()).apply {
            paint.color = ColorUtils.compositeColors(
                theme.keyPressHighlightColor,
                theme.accentKeyBackgroundColor
            )
        },
        (keySize - ctx.dp(34)) / 2
    )

    private val columnCount = items.size
    private val focusColumn = calcInitialFocusedColumn(columnCount, keySize, bounds)

    override val offsetX = ((bounds.width() - keySize) / 2) - (keySize * focusColumn)
    override val offsetY = ctx.dp(-52)

    private val columnOrder = createColumnOrder(columnCount, focusColumn)

    private var focusedIndex = columnOrder[focusColumn]

    private val keyViews = items.map {
        imageView {
            background = inactiveBackground
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageDrawable = drawable(it.icon)!!.apply {
                setTint(theme.accentKeyTextColor)
            }
        }
    }

    init {
        markFocus(focusedIndex)
    }

    override val root = horizontalLayout root@{
        for (i in 0 until columnCount) {
            val view = keyViews[columnOrder[i]]
            add(view, lParams(keySize, keySize))
        }
    }

    private fun markFocus(index: Int) {
        keyViews.getOrNull(index)?.apply {
            background = activeBackground
        }
    }

    private fun markInactive(index: Int) {
        keyViews.getOrNull(index)?.apply {
            background = inactiveBackground
        }
    }

    override fun onChangeFocus(x: Float, y: Float): Boolean {
        var newColumn = floor(x / keySize).toInt()
        if (newColumn < -2 || newColumn > columnCount + 1) {
            onDismissSelf(this)
            return true
        }
        newColumn = limitIndex(newColumn, columnCount)
        val newFocus = columnOrder[newColumn]
        if (newFocus < keyViews.size) {
            markInactive(focusedIndex)
            markFocus(newFocus)
            focusedIndex = newFocus
        }
        return false
    }

    override fun onTrigger() = items.getOrNull(focusedIndex)?.action
}
