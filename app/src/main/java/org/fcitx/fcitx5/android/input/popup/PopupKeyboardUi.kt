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
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * @param ctx [Context]
 * @param theme [Theme]
 * @param keyWidth key width in popup keyboard
 * @param popupHeight popup preview view height. Used to transform gesture coordinate from
 * trigger view to popup keyboard view. See [changeFocus].
 * @param keyHeight key height in popup keyboard
 * @param radius popup keyboard and key radius
 * @param bounds bound [Rect] of popup trigger view. Used to calculate free space of both sides and
 * determine column order. See [focusColumn] and [columnOrder].
 * @param keys symbols to show on popup keyboard
 * @param onDismissSelf callback when popup keyboard wants to close
 */
class PopupKeyboardUi(
    override val ctx: Context,
    private val theme: Theme,
    private val keyWidth: Int,
    popupHeight: Int,
    private val keyHeight: Int,
    radius: Float,
    bounds: Rect,
    private val keys: Array<String>,
    labels: Array<String>,
    private val onDismissSelf: PopupKeyboardUi.() -> Unit = {}
) : Ui {

    private val inactiveBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.popupBackgroundColor.color)
    }

    private val focusBackground = GradientDrawable().apply {
        cornerRadius = radius
        setColor(theme.genericActiveBackgroundColor.color)
    }

    private val rowCount: Int
    private val columnCount: Int

    // those 2 variables meas initial focus row/column during initialization
    private val focusRow: Int
    private val focusColumn: Int

    init {
        val keyCount: Float = keys.size.toFloat()
        rowCount = keyCount.let {
            for (i in 1 until 5) {
                if (it / i <= 5f) return@let i
            }
            return@let 1
        }
        columnCount = (keyCount / rowCount).roundToInt()

        val leftSpace = bounds.left
        val rightSpace = ctx.resources.displayMetrics.widthPixels - bounds.right
        var col = (columnCount - 1) / 2
        while (keyWidth * col > leftSpace) col--
        while (keyWidth * (columnCount - col - 1) > rightSpace) col++

        focusRow = 0
        focusColumn = col
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

    private val keyViews = labels.map {
        textView {
            text = it
            textSize = 23f
            gravity = gravityCenter
            setTextColor(theme.keyTextColor.color)
        }
    }

    init {
        markFocus(focusedIndex)
    }

    override val root = verticalLayout root@{
        background = inactiveBackground
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(2f)
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
            }, lParams(width = matchParent))
        }
    }

    private fun markFocus(index: Int) {
        keyViews.getOrNull(index)?.apply {
            background = focusBackground
            setTextColor(theme.genericActiveForegroundColor.color)
        }
    }

    private fun markInactive(index: Int) {
        keyViews.getOrNull(index)?.apply {
            background = null
            setTextColor(theme.popupTextColor.color)
        }
    }

    private val gestureOffsetX = (keyWidth - bounds.width()) / 2
    private val gestureOffsetY = popupHeight - keyHeight - bounds.height()

    /**
     * Change focused view of popup keyboard according to gesture coordinate.
     *
     * Gesture events usually come from popup trigger view (eg. KeyView), and its coordinate is
     * relative to trigger view's top-left corner.
     *
     * The X coordinate (horizontal) should be offset by half of the difference of `popupWidth` and
     * `bounds.width()`, [gestureOffsetX].
     *
     * The Y coordinate (vertical) should first be inverted (because popup keyboard shows above
     * trigger view, when pointer moves to popup keyboard, the value is negative), and the offset
     * by a distance, which comes [gestureOffsetY].
     *
     * ```
     *            ┌─── ┌─ ┌───┬───┐
     *    popupKeyHeight  │ @ │ A │
     *            │    └─ ├───┼───┘ ─┐
     * popupHeight│       │   │      │gestureOffsetX
     *            │    ┌─ │o─┐│ ─────┘
     *    bounds.height() ││a││
     *            │    └─ │└─┘│
     *            └────── └───┘
     * o: gesture coordinate origin
     * ```
     * @param x gesture X coordinate (relative to trigger view)
     * @param y gesture Y coordinate (relative to trigger view)
     *
     * @return Whether the gesture should be consumed, ie. no more gesture events should
     * be dispatched to the trigger view.
     */
    fun changeFocus(x: Float, y: Float): Boolean {
        // round(position / height + 0.2): move to next row when gesture moves above 30% of current row
        var newRow = ((-y - gestureOffsetY) / keyHeight + 0.2).roundToInt() + focusRow
        // floor(position / width): move to next column when gesture moves out of current column
        var newColumn = floor((x + gestureOffsetX) / keyWidth).toInt() + focusColumn
        // retain focus when gesture moves between ±2 rows/columns of range
        if (newRow < -2 || newRow > rowCount + 1 || newColumn < -2 || newColumn > columnCount + 1) {
            onDismissSelf(this)
            return true
        }
        newRow = limitIndex(newRow, rowCount)
        newColumn = limitIndex(newColumn, columnCount)
        val newFocus = keyOrders[newRow][newColumn]
        if (newFocus < keyViews.size) {
            markInactive(focusedIndex)
            markFocus(newFocus)
            focusedIndex = newFocus
            return true
        }
        return false
    }

    fun trigger() = keys.getOrNull(focusedIndex)?.let { KeyAction.FcitxKeyAction(it) }

    companion object {
        private fun limitIndex(input: Int, limit: Int) =
            if (input < 0) 0 else if (input >= limit) limit - 1 else input
    }
}
