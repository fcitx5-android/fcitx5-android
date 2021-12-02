package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.AppSharedPreferences
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.inputConnection
import me.rocka.fcitx5test.keyboard.layout.*
import me.rocka.fcitx5test.native.InputMethodEntry
import splitties.systemservices.inputMethodManager
import splitties.systemservices.windowManager

class KeyboardView(
    private val service: FcitxInputMethodService,
    private val preeditBinding: KeyboardPreeditBinding
) : KeyboardContract.View {

    private val preeditPopup = PopupWindow(
        preeditBinding.root,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    ).apply {
        isTouchable = false
        isClippingEnabled = false
    }
    var keyboardView: CustomKeyboardView
    private lateinit var candidateLytMgr: RecyclerView.LayoutManager
    private lateinit var candidateViewAdp: CandidateViewAdapter

    lateinit var presenter: KeyboardPresenter

    init {
        preeditPopup.width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            DisplayMetrics().let {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(it)
                it.widthPixels
            }
        }
        keyboardView = createKeyboardView(Preset.Qwerty)
    }

    private fun createKeyboardView(layout: List<List<BaseButton>>): CustomKeyboardView {
        return Factory.create(service.applicationContext, layout) { v, it, long ->
            if (AppSharedPreferences.getInstance().buttonHapticFeedback && (!long)) {
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
                is ButtonAction.UnicodeAction -> presenter.unicode()
                is ButtonAction.LangSwitchAction -> presenter.switchLang()
                is ButtonAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
                is ButtonAction.ReturnAction -> presenter.enter()
                is ButtonAction.CustomAction -> presenter.customEvent(it.act)
                is ButtonAction.LayoutSwitchAction -> TODO()
            }
        }.apply {
            candidateList?.run {
                candidateLytMgr = LinearLayoutManager(service, LinearLayoutManager.HORIZONTAL, false)
                layoutManager = candidateLytMgr
                candidateViewAdp = CandidateViewAdapter()
                candidateViewAdp.onSelectCallback = { idx -> presenter.selectCandidate(idx) }
                adapter = candidateViewAdp
            }
            backspace?.run {
                setOnTouchListener { v, e ->
                    when (e.action) {
                        MotionEvent.ACTION_BUTTON_PRESS -> v.performClick()
                        MotionEvent.ACTION_UP -> presenter.stopDeleting()
                    }
                    false
                }
                setOnLongClickListener {
                    presenter.startDeleting()
                    true
                }
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
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            val height = preeditBinding.root.run {
                measure(widthSpec, heightSpec)
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

    @SuppressLint("NotifyDataSetChanged")
    override fun updateCandidates(data: List<String>) {
        candidateViewAdp.candidates = data
        candidateViewAdp.notifyDataSetChanged()
        candidateLytMgr.scrollToPosition(0)
    }

    override fun updateCapsButtonState(state: KeyboardContract.CapsState) {
        keyboardView.caps?.setImageResource(
            when (state) {
                KeyboardContract.CapsState.None -> R.drawable.ic_baseline_keyboard_capslock0_24
                KeyboardContract.CapsState.Once -> R.drawable.ic_baseline_keyboard_capslock1_24
                KeyboardContract.CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock2_24
            }
        )
    }

    override fun updateSpaceButtonText(entry: InputMethodEntry) {
        keyboardView.space?.text = entry.displayName
    }
}