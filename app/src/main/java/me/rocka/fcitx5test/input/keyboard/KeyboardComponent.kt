package me.rocka.fcitx5test.input.keyboard

import android.widget.FrameLayout
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.FcitxInputMethodService
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.utils.dependency.UniqueViewComponent
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.dependency.inputMethodService
import splitties.resources.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

class KeyboardComponent : UniqueViewComponent<KeyboardComponent, FrameLayout>() {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.inputMethodService()
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

    fun switchLayout(to: String, listener: BaseKeyboard.KeyActionListener) {
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
        view.add(
            currentKeyboard,
            FrameLayout.LayoutParams(currentKeyboard.matchParent, currentKeyboard.wrapContent)
        )
        currentKeyboard.keyActionListener = listener
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    fun updateCurrentIme(ime: InputMethodEntry) {
        _currentIme = ime
        currentKeyboard.onInputMethodChange(currentIme)
    }

    fun showKeyboard() {
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    fun updatePreedit(content: PreeditContent) {
        currentKeyboard.onPreeditChange(service.editorInfo, content)
    }

}