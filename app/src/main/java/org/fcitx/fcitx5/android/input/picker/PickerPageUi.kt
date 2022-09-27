package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.ImageKeyView
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SymAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener.Source
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.keyboard.TextKeyView
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent

class PickerPageUi(override val ctx: Context, val theme: Theme) : Ui {

    companion object {
        val SymbolAppearance = Appearance.Text(
            displayText = "",
            textSize = 19f,
            typeface = Typeface.NORMAL,
            variant = Variant.Normal,
            border = Border.Off
        )

        val BackspaceAppearance = Appearance.Image(
            src = R.drawable.ic_baseline_backspace_24,
            variant = Variant.Alternative,
            border = Border.Off,
            viewId = R.id.button_backspace
        )

        val BackspaceAction = SymAction(0xff08u)

        // TODO: configurable grid size
        const val RowCount = 3
        const val ColumnCount = 10
    }

    var keyActionListener: KeyActionListener? = null

    private val keyViews = Array(28) {
        TextKeyView(ctx, theme, SymbolAppearance)
    }

    private val backspaceKey = ImageKeyView(ctx, theme, BackspaceAppearance).apply {
        setOnClickListener { onBackspaceClick() }
        repeatEnabled = true
        onRepeatListener = { onBackspaceClick() }
    }

    private fun onBackspaceClick() {
        keyActionListener?.onKeyAction(BackspaceAction, Source.Keyboard)
    }

    override val root = constraintLayout {
        val keyWidth = 1f / ColumnCount
        keyViews.forEachIndexed { i, keyView ->
            val row = i / ColumnCount
            val column = i % ColumnCount
            add(keyView, lParams {
                // layout_constraintTop_to
                if (row == 0) {
                    // first row, align top to top of parent
                    topOfParent()
                } else {
                    // not first row, align top to bottom of first view in last row
                    topToBottomOf(keyViews[(row - 1) * ColumnCount])
                }
                // layout_constraintBottom_to
                if (row == RowCount - 1) {
                    // last row, align bottom to bottom of parent
                    bottomOfParent()
                } else {
                    // not last row, align bottom to top of first view in next row
                    bottomToTopOf(keyViews[(row + 1) * ColumnCount])
                }
                // layout_constraintEnd_to
                if (i == keyViews.size - 1) {
                    // last key (likely not last column), align end to start of backspace button
                    endToStartOf(backspaceKey)
                } else if (column == ColumnCount - 1) {
                    // last column, align end to end of parent
                    endOfParent()
                } else {
                    // neither, align end to start of next view
                    endToStartOf(keyViews[i + 1])
                }
                matchConstraintPercentWidth = keyWidth
            })
        }
        add(backspaceKey, lParams {
            // top to bottom of first view of second-last row
            below(keyViews[(RowCount - 2) * ColumnCount])
            // bottom/right corner
            bottomOfParent()
            endOfParent()
            matchConstraintPercentWidth = 0.15f
        })
        // Pages must fill the whole ViewPager2 (use match_parent)
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
    }

    private fun onSymbolClick(str: String) {
        keyActionListener?.onKeyAction(CommitAction(str), Source.Keyboard)
    }

    fun setItems(items: Array<String>, insertRecentlyUsed: (String) -> Unit) {
        keyViews.forEachIndexed { i, keyView ->
            keyView.apply {
                if (i >= items.size) {
                    isEnabled = false
                    mainText.text = ""
                    setOnClickListener(null)
                } else {
                    isEnabled = true
                    val text = items[i]
                    mainText.text = text
                    setOnClickListener {
                        insertRecentlyUsed(text)
                        onSymbolClick(text)
                    }
                }
            }
        }
    }
}
