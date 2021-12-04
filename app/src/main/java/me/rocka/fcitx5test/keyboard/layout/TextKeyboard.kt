package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import me.rocka.fcitx5test.native.InputMethodEntry

class TextKeyboard(
    context: Context,
    private val fcitx: Fcitx,
    passAction: (View, KeyAction<*>, Boolean) -> Unit
) : BaseKeyboard(context, fcitx, Preset.Qwerty, passAction) {

    enum class CapsState { None, Once, Lock }

    val caps: ImageButton by lazy { findViewById(R.id.button_caps) }
    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val layoutSwitch: ImageButton by lazy { findViewById(R.id.button_layout_switch) }
    val quickphrase: ImageButton by lazy { findViewById(R.id.button_quickphrase) }
    val lang: ImageButton by lazy { findViewById(R.id.button_lang) }
    val space: Button by lazy { findViewById(R.id.button_space) }
    val `return`: Button by lazy { findViewById(R.id.button_return) }

    var capsState: CapsState = CapsState.None

    init {
        backspace.run {
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                    MotionEvent.ACTION_UP -> stopDeleting()
                }
                false
            }
            setOnLongClickListener {
                startDeleting()
                true
            }
        }
    }

    override fun handleFcitxEvent(event: FcitxEvent<*>) = when (event) {
        is FcitxEvent.ReadyEvent -> {
            updateSpaceButtonText(fcitx.ime())
        }
        is FcitxEvent.IMChangeEvent -> {
            updateSpaceButtonText(event.data.status)
        }
        else -> {}
    }

    override fun onAction(v: View, it: KeyAction<*>, long: Boolean) {
        super.onAction(v, it, long)
        when (it) {
            is KeyAction.FcitxKeyAction -> onKeyPress(it.act)
            is KeyAction.CapsAction -> switchCapsState()
            is KeyAction.BackspaceAction -> backspace()
            is KeyAction.QuickPhraseAction -> quickPhrase()
            is KeyAction.UnicodeAction -> unicode()
            is KeyAction.LangSwitchAction -> switchLang()
            is KeyAction.ReturnAction -> enter()
            is KeyAction.CustomAction -> customEvent(it.act)
            else -> {}
        }
    }

    override fun onKeyPress(key: String) {
        val k = key[0]
        val c = when (capsState) {
            CapsState.None -> {
                k.lowercaseChar()
            }
            CapsState.Once -> {
                capsState = CapsState.None
                updateCapsButtonIcon()
                k.uppercaseChar()
            }
            CapsState.Lock -> {
                k.uppercaseChar()
            }
        }
        fcitx.sendKey(c)
    }

    private fun switchCapsState() {
        capsState = when (capsState) {
            CapsState.None -> CapsState.Once
            CapsState.Once -> CapsState.Lock
            CapsState.Lock -> CapsState.None
        }
        updateCapsButtonIcon()
    }

    private fun updateCapsButtonIcon() {
        caps.setImageResource(
            when (capsState) {
                CapsState.None -> R.drawable.ic_baseline_keyboard_capslock0_24
                CapsState.Once -> R.drawable.ic_baseline_keyboard_capslock1_24
                CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock2_24
            }
        )
    }

    private fun updateSpaceButtonText(entry: InputMethodEntry) {
        space.text = entry.displayName
    }

}