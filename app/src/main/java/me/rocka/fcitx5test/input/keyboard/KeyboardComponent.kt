package me.rocka.fcitx5test.input.keyboard

import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.FcitxInputMethodService
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.dependency.fcitx
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.utils.AppUtil
import me.rocka.fcitx5test.utils.inputConnection
import splitties.resources.str
import splitties.systemservices.inputMethodManager
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

class KeyboardComponent : UniqueViewComponent<KeyboardComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val fcitx by manager.fcitx()


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
        view.add(
            currentKeyboard,
            FrameLayout.LayoutParams(currentKeyboard.matchParent, currentKeyboard.wrapContent)
        )
        currentKeyboard.keyActionListener = listener
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


    fun showKeyboard() {
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    // TODO: We expose this listener share with expandable candidate.
    //  However, expandable candidate shouldn't have been a keyboard.
    //  See the note on ExpandableCandidateLayout for details.
    val listener = BaseKeyboard.KeyActionListener { action ->
        service.lifecycleScope.launch {
            when (action) {
                is KeyAction.FcitxKeyAction -> fcitx.sendKey(action.act)
                is KeyAction.CommitAction -> {
                    // TODO: this should be handled more gracefully; or CommitAction should be removed?
                    fcitx.reset()
                    service.inputConnection?.commitText(action.act, 1)
                }
                is KeyAction.RepeatStartAction -> service.startRepeating(action.act)
                is KeyAction.RepeatEndAction -> service.cancelRepeating(action.act)
                is KeyAction.QuickPhraseAction -> {
                    fcitx.reset()
                    fcitx.triggerQuickPhrase()
                }
                is KeyAction.UnicodeAction -> {
                    fcitx.triggerUnicode()
                }
                is KeyAction.LangSwitchAction -> {
                    if (fcitx.enabledIme().size < 2) {
                        AppUtil.launchMainToAddInputMethods(context)
                    } else
                        fcitx.enumerateIme()
                }
                is KeyAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
                is KeyAction.LayoutSwitchAction -> switchLayout(action.act)
                is KeyAction.CustomAction -> {
                    action.act(fcitx)
                }
                else -> {
                }
            }
        }
    }

}