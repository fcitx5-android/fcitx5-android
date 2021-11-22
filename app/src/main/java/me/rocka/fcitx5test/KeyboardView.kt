package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.databinding.QwertyKeyboardBinding
import me.rocka.fcitx5test.native.InputMethodEntry


class KeyboardView(
    val service: FcitxIMEService,
    val keyboardBinding: QwertyKeyboardBinding,
    val preeditBinding: KeyboardPreeditBinding
) :
    KeyboardContract.View {

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

            buttonCaps.setOnClickListener { presenter.switchCapsState() }

            buttonBackspace.let {
                it.setOnTouchListener { v, e ->
                    when (e.action) {
                        MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                        MotionEvent.ACTION_UP -> presenter.stopDeleting()
                    }
                    false
                }
                it.setOnClickListener { presenter.backspace() }
                it.setOnLongClickListener {
                    presenter.startDeleting()
                    true
                }
            }

            buttonLang.let {
                it.setOnClickListener { presenter.switchLang() }
                it.setOnLongClickListener {
                    (service.getSystemService(InputMethodService.INPUT_METHOD_SERVICE)
                            as InputMethodManager).showInputMethodPicker()
                    true
                }
            }

            root.allChildren()
                // unstable, assuming all letter keys names have the pattern: button_X
                .filter { it.resources.getResourceName(it.id).takeLast(2).startsWith('_') }
                .mapNotNull { it as? Button }
                .forEach { it.setOnClickListener { _ -> presenter.onKeyPress(it.text[0]) } }

            buttonQuickphrase.setOnClickListener { presenter.quickPhrase() }

            buttonSpace.setOnClickListener { presenter.space() }

            buttonPunctuation.setOnClickListener { presenter.punctuation() }

            buttonEnter.setOnClickListener { presenter.enter() }
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

    override fun updateSpaceButtonText(entry: InputMethodEntry) {
        keyboardBinding.buttonSpace.text = entry.name
    }

}