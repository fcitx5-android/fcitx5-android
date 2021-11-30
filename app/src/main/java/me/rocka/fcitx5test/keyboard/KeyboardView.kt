package me.rocka.fcitx5test.keyboard

import android.inputmethodservice.InputMethodService
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.MyOnClickListener
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.allChildren
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.databinding.QwertyKeyboardBinding
import me.rocka.fcitx5test.native.InputMethodEntry


class KeyboardView(
    val service: FcitxInputMethodService,
    val keyboardBinding: QwertyKeyboardBinding,
    val preeditBinding: KeyboardPreeditBinding
) :
    KeyboardContract.View, MyOnClickListener {

    private val candidateLytMgr =
        LinearLayoutManager(service, LinearLayoutManager.HORIZONTAL, false)
    private val candidateViewAdp = CandidateViewAdapter()

    lateinit var presenter: KeyboardPresenter

    init {
        with(keyboardBinding) {

            candidateList.let {
                it.layoutManager = candidateLytMgr
                candidateViewAdp.onSelectCallback = { idx -> presenter.selectCandidate(idx) }
                it.adapter = candidateViewAdp
            }

            buttonCaps.setOnClickListenerWithMe { presenter.switchCapsState() }

            buttonBackspace.let {
                it.setOnTouchListener { v, e ->
                    when (e.action) {
                        MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                        MotionEvent.ACTION_UP -> presenter.stopDeleting()
                    }
                    false
                }
                it.setOnClickListenerWithMe { presenter.backspace() }
                it.setOnLongClickListenerWithMe {
                    presenter.startDeleting()
                    true
                }
            }

            buttonLang.let {
                it.setOnClickListenerWithMe { presenter.switchLang() }
                it.setOnLongClickListenerWithMe {
                    (service.getSystemService(InputMethodService.INPUT_METHOD_SERVICE)
                            as InputMethodManager).showInputMethodPicker()
                    true
                }
            }

            root.allChildren()
                // unstable, assuming all letter keys names have the pattern: button_X
                .filter { it.resources.getResourceName(it.id).takeLast(2).startsWith('_') }
                .mapNotNull { it as? Button }
                .forEach { it.setOnClickListenerWithMe { _ -> presenter.onKeyPress(it.text[0]) } }

            buttonQuickphrase.setOnClickListenerWithMe { presenter.quickPhrase() }

            buttonSpace.setOnClickListenerWithMe { presenter.space() }

            buttonPunctuation.setOnClickListenerWithMe { presenter.punctuation() }

            buttonEnter.setOnClickListenerWithMe { presenter.enter() }

        }

    }

    override fun updatePreedit(data: KeyboardContract.PreeditContent) {
        val start = data.aux.auxUp + data.preedit.preedit
        val end = data.aux.auxDown
        val hasStart = start.isNotEmpty()
        val hasEnd = end.isNotEmpty()
        service.setCandidatesViewShown(hasStart or hasEnd)
        with(preeditBinding) {
            keyboardPreeditText.alpha = if (hasStart) 1f else 0f
            keyboardPreeditAfterText.alpha = if (hasEnd) 1f else 0f
            keyboardPreeditText.text = start
            keyboardPreeditAfterText.text = end
        }
    }

    override fun updateCandidates(data: List<String>) {
        candidateViewAdp.candidates = data
        candidateViewAdp.notifyDataSetChanged()
        candidateLytMgr.scrollToPosition(0)
    }

    override fun updateCapsButtonState(state: KeyboardContract.CapsState) {
        // TODO: if system color scheme changes, capslock1 icon won't be recolored; why?
        keyboardBinding.buttonCaps.setImageResource(when (state) {
            KeyboardContract.CapsState.None -> R.drawable.ic_baseline_keyboard_capslock0_24
            KeyboardContract.CapsState.Once -> R.drawable.ic_baseline_keyboard_capslock1_24
            KeyboardContract.CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock2_24
        })
    }

    override fun updateSpaceButtonText(entry: InputMethodEntry) {
        keyboardBinding.buttonSpace.text = entry.displayName
    }

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun onLongClick(v: View): Boolean {
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        return true
    }

}