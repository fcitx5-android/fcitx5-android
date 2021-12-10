package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.AppSharedPreferences
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.inputConnection
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.keyboard.layout.NumberKeyboard
import me.rocka.fcitx5test.keyboard.layout.TextKeyboard
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.systemservices.inputMethodManager
import splitties.systemservices.layoutInflater
import splitties.systemservices.windowManager
import splitties.views.backgroundColor
import splitties.views.dsl.core.*

class InputView(
    val service: FcitxInputMethodService,
    val fcitx: Fcitx
) : LinearLayout(service) {

    data class PreeditContent(
        var preedit: FcitxEvent.PreeditEvent.Data,
        var aux: FcitxEvent.InputPanelAuxEvent.Data
    )

    private val themedContext = context.withTheme(R.style.Theme_AppCompat_DayNight)

    private var cachedPreedit = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )
    private val preeditBinding = KeyboardPreeditBinding.inflate(themedContext.layoutInflater)
    private var preeditPopup = PopupWindow(
        preeditBinding.root,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    ).apply {
        isTouchable = false
        isClippingEnabled = false
    }

    private var candidateLytMgr = LinearLayoutManager(context).apply {
        orientation = LinearLayoutManager.HORIZONTAL
    }
    private var candidateViewAdp = CandidateViewAdapter { fcitx.select(it) }
    private var candidateView = themedContext.view(::RecyclerView, R.id.candidate_list) {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        layoutManager = candidateLytMgr
        adapter = candidateViewAdp
    }

    private var keyboards: HashMap<String, BaseKeyboard> = hashMapOf(
        "qwerty" to TextKeyboard(themedContext),
        "number" to NumberKeyboard(themedContext)
    )
    private var currentKeyboardName = "qwerty"
    private val currentKeyboard: BaseKeyboard get() = keyboards.getValue(currentKeyboardName)

    private val keyActionListener = BaseKeyboard.KeyActionListener { view, action, long ->
        onAction(view, action, long)
    }

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
        orientation = VERTICAL
        add(candidateView, lParams(matchParent, dp(40)))
        add(currentKeyboard, lParams(matchParent, wrapContent))
        currentKeyboard.keyActionListener = keyActionListener
        currentKeyboard.onAttach()
        currentKeyboard.onInputMethodChange(fcitx.ime())
    }

    fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                updateCandidates(it.data)
            }
            is FcitxEvent.PreeditEvent -> it.data.let {
                cachedPreedit.preedit = it
                updatePreedit(cachedPreedit)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                cachedPreedit.aux = it.data
                updatePreedit(cachedPreedit)
            }
            is FcitxEvent.ReadyEvent -> {
                currentKeyboard.onInputMethodChange(fcitx.ime())
            }
            is FcitxEvent.IMChangeEvent -> {
                currentKeyboard.onInputMethodChange(it.data.status)
            }
            else -> {}
        }
    }

    private fun onAction(view: View, action: KeyAction<*>, long: Boolean) {
        if (AppSharedPreferences.getInstance().buttonHapticFeedback && (!long)) {
            // TODO: write our own button to handle haptic feedback for both tap and long click
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        when (action) {
            is KeyAction.FcitxKeyAction -> fcitx.sendKey(action.act)
            is KeyAction.CommitAction -> {
                // TODO: this should be handled more gracefully; or CommitAction should be removed?
                fcitx.reset()
                service.inputConnection?.commitText(action.act, 1)
            }
            is KeyAction.QuickPhraseAction -> quickPhrase()
            is KeyAction.UnicodeAction -> unicode()
            is KeyAction.LangSwitchAction -> switchLang()
            is KeyAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
            is KeyAction.LayoutSwitchAction -> switchLayout(action.act)
            is KeyAction.CustomAction -> customEvent(action.act)
            else -> {}
        }
    }

    fun updatePreedit(data: PreeditContent) {
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
            if ((!hasStart) && (!hasEnd)) {
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
                showAtLocation(this@InputView, Gravity.NO_GRAVITY, 0, -height)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCandidates(data: List<String>) {
        candidateViewAdp.candidates = data
        candidateViewAdp.notifyDataSetChanged()
        candidateLytMgr.scrollToPosition(0)
    }

    private fun switchLayout(to: String) {
        currentKeyboard.keyActionListener = null
        currentKeyboard.onDetach()
        removeView(currentKeyboard)
        currentKeyboardName = if (to.isNotEmpty()) {
            to
        } else when (currentKeyboardName) {
            "qwerty" -> "number"
            else -> "qwerty"
        }
        add(currentKeyboard, lParams(matchParent, wrapContent))
        currentKeyboard.keyActionListener = keyActionListener
        currentKeyboard.onAttach()
        currentKeyboard.onInputMethodChange(fcitx.ime())
    }

    private fun quickPhrase() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
    }

    private fun unicode() {
        fcitx.triggerUnicode()
    }

    private fun switchLang() {
        val list = fcitx.enabledIme()
        if (list.isEmpty()) return
        val status = fcitx.ime()
        val index = list.indexOfFirst { it.uniqueName == status.uniqueName }
        val next = list[(index + 1) % list.size]
        fcitx.activateIme(next.uniqueName)
    }

    private fun customEvent(fn: (Fcitx) -> Unit) {
        fn(fcitx)
    }
}
