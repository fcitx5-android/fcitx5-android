package me.rocka.fcitx5test.keyboard

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.inputConnection
import me.rocka.fcitx5test.keyboard.layout.ButtonAction
import me.rocka.fcitx5test.keyboard.layout.CustomKeyboardView
import me.rocka.fcitx5test.keyboard.layout.Factory
import me.rocka.fcitx5test.keyboard.layout.Preset
import me.rocka.fcitx5test.native.InputMethodEntry
import me.rocka.fcitx5test.registerSharedPerfChangeListener
import me.rocka.fcitx5test.settings.PreferenceKeys

class KeyboardView(
    val service: FcitxInputMethodService,
    private val preeditBinding: KeyboardPreeditBinding
) : KeyboardContract.View, SharedPreferences.OnSharedPreferenceChangeListener {

    private val preeditPopup = PopupWindow(
        preeditBinding.root,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    ).apply {
        isTouchable = false
        isClippingEnabled = false
    }
    var keyboardView: CustomKeyboardView
    private val candidateLytMgr =
        LinearLayoutManager(service, LinearLayoutManager.HORIZONTAL, false)
    private val candidateViewAdp = CandidateViewAdapter()

    lateinit var presenter: KeyboardPresenter

    private var hapticFeedback = true

    init {
        val context = service.applicationContext
        context.registerSharedPerfChangeListener(this, PreferenceKeys.ButtonHapticFeedback)
        keyboardView = Factory.create(context, Preset.Qwerty) { v, it, long ->
            if (hapticFeedback and (!long)) {
                // TODO: write our own button to handle haptic feedback for both tap and long click
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            when (it) {
                is ButtonAction.FcitxKeyAction -> presenter.onKeyPress(it.act[0])
                is ButtonAction.CommitAction -> {
                    // TODO: this should be handled more gracefully
                    presenter.reset()
                    service.inputConnection?.commitText(it.act, 1)
                }
                is ButtonAction.CapsAction -> presenter.switchCapsState()
                is ButtonAction.BackspaceAction -> presenter.backspace()
                is ButtonAction.QuickPhraseAction -> presenter.quickPhrase()
                is ButtonAction.LangSwitchAction -> presenter.switchLang()
                is ButtonAction.InputMethodSwitchAction ->
                    (service.getSystemService(InputMethodService.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showInputMethodPicker()
                is ButtonAction.ReturnAction -> presenter.enter()
                is ButtonAction.CustomAction -> presenter.customEvent(it.act)
            }
        }
        keyboardView.candidateList.run {
            layoutManager = candidateLytMgr
            candidateViewAdp.onSelectCallback = { idx -> presenter.selectCandidate(idx) }
            adapter = candidateViewAdp
        }
        keyboardView.backspace.let {
            it.setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                    MotionEvent.ACTION_UP -> presenter.stopDeleting()
                }
                false
            }
            it.setOnLongClickListener {
                presenter.startDeleting()
                true
            }
        }
    }

    override fun updatePreedit(data: KeyboardContract.PreeditContent) {
        val start = data.aux.auxUp + data.preedit.preedit
        val end = data.aux.auxDown
        val hasStart = start.isNotEmpty()
        val hasEnd = end.isNotEmpty()
        preeditBinding.run {
            keyboardPreeditText.alpha = if (hasStart) 1f else 0f
            keyboardPreeditAfterText.alpha = if (hasEnd) 1f else 0f
            keyboardPreeditText.text = start
            keyboardPreeditAfterText.text = end
        }
        preeditPopup.run {
            if ((!hasStart) and (!hasEnd)) {
                dismiss()
                return
            }
            // FIXME: it can only measure height of 1 line
            val height = preeditBinding.root.run {
                measure(0, 0)
                measuredHeight
            }
            if (isShowing) {
                update(
                    0, -height,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            } else {
                showAtLocation(keyboardView.root, Gravity.NO_GRAVITY, 0, -height)
            }
        }
    }

    override fun updateCandidates(data: List<String>) {
        candidateViewAdp.candidates = data
        candidateViewAdp.notifyDataSetChanged()
        candidateLytMgr.scrollToPosition(0)
    }

    override fun updateCapsButtonState(state: KeyboardContract.CapsState) {
        keyboardView.caps.setImageResource(
            when (state) {
                KeyboardContract.CapsState.None -> R.drawable.ic_baseline_keyboard_capslock0_24
                KeyboardContract.CapsState.Once -> R.drawable.ic_baseline_keyboard_capslock1_24
                KeyboardContract.CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock2_24
            }
        )
    }

    override fun updateSpaceButtonText(entry: InputMethodEntry) {
        keyboardView.space.text = entry.displayName
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key) {
            PreferenceKeys.ButtonHapticFeedback -> {
                hapticFeedback = pref.getBoolean(key, true)
            }
        }
    }

}