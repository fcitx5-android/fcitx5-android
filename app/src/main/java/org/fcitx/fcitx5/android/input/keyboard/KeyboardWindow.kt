package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must
import splitties.resources.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent

class KeyboardWindow : InputWindow.SimpleInputWindow<KeyboardWindow>(),
    InputBroadcastReceiver {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()
    private val theme by manager.theme()

    private var _currentIme: InputMethodEntry? = null

    val currentIme
        get() = _currentIme ?: InputMethodEntry(context.str(R.string._not_available_))

    private lateinit var keyboardView: FrameLayout

    private val keyboards: HashMap<String, BaseKeyboard> by lazy {
        hashMapOf(
            // a placeholder layout to avoid crash
            EMPTY to (object : BaseKeyboard(context, theme, listOf(listOf())) {}),
            TextKeyboard.Name to TextKeyboard(context, theme),
            NumberKeyboard.Name to NumberKeyboard(context, theme),
            NumSymKeyboard.Name to NumSymKeyboard(context, theme),
            SymbolKeyboard.Name to SymbolKeyboard(context, theme)
        )
    }
    private var currentKeyboardName = EMPTY
    private var lastSymbolType: String by AppPrefs.getInstance().internal.lastSymbolLayout

    private val currentKeyboard: BaseKeyboard get() = keyboards.getValue(currentKeyboardName)

    private val keyActionListener = BaseKeyboard.KeyActionListener {
        if (it is KeyAction.LayoutSwitchAction) {
            switchLayout(it.act)
        } else {
            commonKeyActionListener.listener.onKeyAction(it)
        }
    }

    private val keyPopupListener = object : BaseKeyboard.KeyPopupListener {
        override fun onPreview(viewId: Int, content: String, bounds: Rect) {
            popup.showPopup(viewId, content, bounds)
        }

        override fun onDismiss(viewId: Int) {
            popup.dismissPopup(viewId)
        }

        override fun onShowKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
            popup.showKeyboard(viewId, keyboard, bounds)
        }

        override fun onChangeFocus(viewId: Int, deltaX: Int, deltaY: Int): Boolean {
            return popup.changeFocus(viewId, deltaX, deltaY)
        }

        override fun onKeyAction(viewId: Int): KeyAction? {
            return popup.triggerFocusedKeyboard(viewId)
        }
    }

    // This will be called EXACTLY ONCE
    override fun onCreateView(): View {
        keyboardView = context.frameLayout(R.id.keyboard_view)
        return keyboardView
    }

    private fun detachCurrentLayout() {
        keyboards[currentKeyboardName]?.also {
            it.onDetach()
            keyboardView.removeView(it)
            it.keyActionListener = null
            it.keyPopupListener = null
        }
    }

    private fun attachLayout(target: String) {
        currentKeyboardName = target
        if (target != TextKeyboard.Name && target != lastSymbolType) {
            lastSymbolType = target
        }
        keyboardView.apply { add(currentKeyboard, lParams(matchParent, matchParent)) }
        currentKeyboard.keyActionListener = keyActionListener
        currentKeyboard.keyPopupListener = keyPopupListener
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    private fun switchLayout(to: String) {
        if (to == currentKeyboardName) return
        ContextCompat.getMainExecutor(service).execute {
            detachCurrentLayout()
            attachLayout(to.ifEmpty { lastSymbolType })
        }
    }

    override fun onEditorInfoUpdate(info: EditorInfo?) {
        switchLayout(service.editorInfo?.inputType?.let {
            when (it and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_NUMBER -> NumberKeyboard.Name
                InputType.TYPE_CLASS_PHONE -> NumberKeyboard.Name
                else -> TextKeyboard.Name
            }
        } ?: TextKeyboard.Name)
        currentKeyboard.onEditorInfoChange(info)
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        _currentIme = ime
        currentKeyboard.onInputMethodChange(currentIme)
    }

    override fun onPreeditUpdate(data: FcitxEvent.PreeditEvent.Data) {
        currentKeyboard.onPreeditChange(service.editorInfo, data)
    }

    override fun onAttached() {
        if (currentKeyboardName === EMPTY) {
            attachLayout(TextKeyboard.Name)
            keyboards.remove(EMPTY)
        } else {
            currentKeyboard.keyActionListener = keyActionListener
            currentKeyboard.keyPopupListener = keyPopupListener
        }
    }

    override fun onDetached() {
        currentKeyboard.keyActionListener = null
        currentKeyboard.keyPopupListener = null
        popup.dismissAll()
    }

    companion object {
        const val EMPTY = ""
    }

}