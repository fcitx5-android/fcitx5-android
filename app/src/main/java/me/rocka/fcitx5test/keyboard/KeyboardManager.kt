package me.rocka.fcitx5test.keyboard

import android.widget.FrameLayout
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.NumberKeyboard
import me.rocka.fcitx5test.keyboard.layout.TextKeyboard
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.dependency.service
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.resources.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

class KeyboardManager : UniqueComponent<KeyboardManager>(), Dependent,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.service()
    private var _currentIme: InputMethodEntry? = null

    val currentIme
        get() = _currentIme ?: InputMethodEntry(context.str(R.string._not_available_))

    val keyboardView by lazy { context.frameLayout(R.id.keyboard_view) }

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
            keyboardView.removeView(it)
        }
        currentKeyboardName = to.ifEmpty {
            when (currentKeyboardName) {
                "qwerty" -> "number"
                else -> "qwerty"
            }
        }
        keyboardView.add(
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