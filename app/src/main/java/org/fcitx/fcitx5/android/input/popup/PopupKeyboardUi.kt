package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityEnd
import splitties.views.gravityStart
import kotlin.math.roundToInt

class PopupKeyboardUi(
    override val ctx: Context,
    private val theme: Theme,
    private val radius: Float,
    private val keys: Array<String>,
    bounds: Rect
) : Ui {

    private val inactiveBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.keyBackgroundColor.color)
    }

    private val focusBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.accentKeyBackgroundColor.color)
    }

    private val keyWidth = ctx.dp(38)
    private val keyHeight = ctx.dp(48)

    private val rowCount: Int
    private val columnCount: Int

    // those 2 variables meas default focus row/column during initialization
    private var focusRow: Int
    private var focusColumn: Int

    init {
        val keyCount: Float = keys.size.toFloat()
        rowCount = keyCount.let {
            for (i in 1 until 5) {
                if (it / i <= 5f) return@let i
            }
            return@let 1
        }
        columnCount = (keyCount / rowCount).roundToInt()

        focusRow = 0
        focusColumn = (columnCount - 1) / 2

        val leftSpace = bounds.left
        val rightSpace = ctx.resources.displayMetrics.widthPixels - bounds.right

        while (keyWidth * focusColumn > leftSpace) focusColumn--
        while (keyWidth * (columnCount - focusColumn - 1) > rightSpace) focusColumn++
    }

    val offsetX = -keyWidth * focusColumn
    val offsetY = -keyHeight * (rowCount - 1)

    /**
     * column order priority: center, right, left. eg.
     * ```
     * | 6 | 4 | 2 | 0 | 1 | 3 | 5 |
     * ```
     * in case free space is not enough in right (left), just skip that cell. eg.
     * ```
     *    | 3 | 2 | 1 | 0 |(no free space)
     * (no free space)| 0 | 1 | 2 | 3 |
     * ```
     */
    private val columnOrder = IntArray(columnCount).also {
        var order = 0
        it[focusColumn] = order++
        for (i in 1 until columnCount * 2) {
            val sign = if (i % 2 == 0) -1 else 1
            val delta = (i / 2f).roundToInt()
            val nextColumn = focusColumn + sign * delta
            if (nextColumn < 0 || nextColumn >= columnCount) continue
            it[nextColumn] = order++
        }
    }

    /**
     * row with smaller index displays at bottom.
     * for example, keyOrders array:
     * ```
     * [[2, 0, 1, 3], [6, 4, 5, 7]]
     * ```
     * displays as
     * ```
     * | 6 | 4 | 5 | 7 |
     * | 2 | 0 | 1 | 3 |
     * ```
     * in which `0` indicates default focus
     */
    private val keyOrders = Array(rowCount) { row ->
        IntArray(columnCount) { col -> row * columnCount + columnOrder[col] }
    }

    private var focusedIndex = keyOrders[focusRow][focusColumn]

    private val keyViews = keys.map {
        textView {
            text = it
            textSize = 23f
            gravity = gravityCenter
            setTextColor(theme.keyTextColor.color)
        }
    }

    override val root = verticalLayout root@{
        background = inactiveBackground
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(2f)
        markFocus(focusedIndex)
        // add rows in reverse order, because newly added view shows at bottom
        for (i in rowCount - 1 downTo 0) {
            val order = keyOrders[i]
            add(horizontalLayout row@{
                for (j in 0 until columnCount) {
                    val view = keyViews.getOrNull(order[j])
                    if (view == null) {
                        // align columns to right (end) when first column is empty, eg.
                        // |   | 6 | 5 | 4 |(no free space)
                        // | 3 | 2 | 1 | 0 |(no free space)
                        gravity = if (j == 0) gravityEnd else gravityStart
                    } else {
                        add(view, lParams(keyWidth, keyHeight))
                    }
                }
            }, lParams {
                // the top most (last) row should to stretch, in case it needs to align right
                if (i == rowCount - 1) {
                    width = matchParent
                }
            })
        }
    }

    private fun markFocus(index: Int) {
        if (index < 0 || index >= keyViews.size) return
        keyViews[index].apply {
            background = focusBackground
            setTextColor(theme.accentKeyTextColor.color)
        }
    }

    private fun markInactive(index: Int) {
        if (index < 0 || index >= keyViews.size) return
        keyViews[index].apply {
            background = null
            setTextColor(theme.keyTextColor.color)
        }
    }

    fun changeFocus(x: Float, y: Float): Boolean {
        // TODO: change key focus by key coordinate
        focusRow = (x / keyWidth).roundToInt()
        focusColumn = (y / keyHeight).roundToInt()
        markInactive(focusedIndex)
        focusedIndex = keyOrders[focusRow][focusColumn]
        // TODO: handle missing keys in grid, fallback to existing ones
        markFocus(focusedIndex)
        return true
    }

    fun trigger() = keys.getOrNull(focusedIndex)?.let { KeyAction.FcitxKeyAction(it) }

    companion object {
        private fun limitIndex(input: Int, limit: Int) =
            if (input < 0) 0 else if (input >= limit) limit - 1 else input
    }
}
