package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.view.ViewGroup
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.*
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener.Source
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.popup.PopupListener
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

        val BackspaceAction = SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace))

        // TODO: configurable grid size
        const val RowCount = 3
        const val ColumnCount = 10

        private var popupOnKeyPress by AppPrefs.getInstance().keyboard.popupOnKeyPress
    }

    var keyActionListener: KeyActionListener? = null
    var popupListener: PopupListener? = null

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

    fun setItems(items: Array<String>) {
        keyViews.forEachIndexed { i, keyView ->
            keyView.apply {
                if (i >= items.size) {
                    isEnabled = false
                    mainText.text = ""
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                    swipeEnabled = false
                    onGestureListener = null
                } else {
                    isEnabled = true
                    val text = items[i]
                    mainText.text = text
                    setOnClickListener {
                        onSymbolClick(text)
                    }
                    setOnLongClickListener { view ->
                        view as KeyView
                        if (!popupOnKeyPress) {
                            view.updateBounds()
                        }
                        onPopupKeyboard(view.id, text, view.bounds)
                        false
                    }
                    swipeEnabled = true
                    onGestureListener = OnGestureListener { view, event ->
                        view as KeyView
                        when (event.type) {
                            CustomGestureView.GestureType.Down -> {
                                if (popupOnKeyPress) {
                                    view.updateBounds()
                                    onPopupPreview(view.id, text, view.bounds)
                                }
                                false
                            }
                            CustomGestureView.GestureType.Move -> {
                                onPopupKeyboardChangeFocus(view.id, event.x, event.y)
                            }
                            CustomGestureView.GestureType.Up -> {
                                onPopupKeyboardTrigger(view.id).also {
                                    onPopupDismiss(view.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onPopupPreview(viewId: Int, content: String, bounds: Rect) {
        popupListener?.onPreview(viewId, content, bounds)
    }

    private fun onPopupDismiss(viewId: Int) {
        popupListener?.onDismiss(viewId)
    }

    private fun onPopupKeyboard(viewId: Int, label: String, bounds: Rect) {
        // TODO: maybe popup keyboard should just accept String as label?
        popupListener?.onShowKeyboard(viewId, KeyDef.Popup.Keyboard(label), bounds)
    }

    private fun onPopupKeyboardChangeFocus(viewId: Int, x: Float, y: Float): Boolean {
        return popupListener?.onChangeFocus(viewId, x, y) ?: false
    }

    private fun onPopupKeyboardTrigger(viewId: Int): Boolean {
        // TODO: maybe popup keyboard should just yield String value?
        val action = popupListener?.onTriggerKeyboard(viewId) as? FcitxKeyAction ?: return false
        onSymbolClick(action.act)
        onPopupDismiss(viewId)
        return true
    }
}
