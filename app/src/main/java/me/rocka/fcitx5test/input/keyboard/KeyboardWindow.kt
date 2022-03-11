package me.rocka.fcitx5test.input.keyboard

import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.FcitxInputMethodService
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.input.wm.InputWindow
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


    private var _currentIme: InputMethodEntry? = null

    val currentIme
        get() = _currentIme ?: InputMethodEntry(context.str(R.string._not_available_))

    override val view by lazy { context.frameLayout(R.id.keyboard_view) }

    private val keyboards: HashMap<String, BaseKeyboard> by lazy {
        hashMapOf(
            "qwerty" to TextKeyboard(context),
            "number" to NumberKeyboard(context)
        )
    }
    private var currentKeyboardName = ""

    val currentKeyboard: BaseKeyboard get() = keyboards.getValue(currentKeyboardName)

    fun switchLayout(to: String) {
        keyboards[currentKeyboardName]?.let {
            it.keyActionListener = null
            it.onDetach()
            view.removeView(it)
        }
        currentKeyboardName = to.ifEmpty {
            when (currentKeyboardName) {
                "qwerty" -> "number"
                else -> "qwerty"
            }
        }
        view.run {
            add(currentKeyboard, lParams(matchParent, matchParent))
        }
        currentKeyboard.keyActionListener = BaseKeyboard.KeyActionListener {
            if (it is KeyAction.LayoutSwitchAction)
                switchLayout(it.act)
            else
                commonKeyActionListener.listener.onKeyAction(it)
        }
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        _currentIme = ime
        currentKeyboard.onInputMethodChange(currentIme)
    }

    override fun onPreeditUpdate(content: PreeditContent) {
        currentKeyboard.onPreeditChange(service.editorInfo, content)
    }


    override fun onShow() {
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    override fun onAttached() {
        switchLayout("qwerty")
    }

    override fun onDetached() {
        currentKeyboard.keyActionListener = null
    }

}