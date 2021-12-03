package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.keyboard.layout.NumberKeyboard
import me.rocka.fcitx5test.keyboard.layout.TextKeyboard
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import splitties.dimensions.dp
import splitties.systemservices.layoutInflater
import splitties.systemservices.windowManager
import splitties.views.dsl.core.*

class InputView(
    service: FcitxInputMethodService,
    val fcitx: Fcitx,
    val passAction: (View, KeyAction<*>, Boolean) -> Unit
) : LinearLayout(service) {

    data class PreeditContent(
        var preedit: FcitxEvent.PreeditEvent.Data,
        var aux: FcitxEvent.InputPanelAuxEvent.Data
    )

    private var cachedPreedit = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )
    private val preeditBinding = KeyboardPreeditBinding.inflate(context.layoutInflater)
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
    private var candidateView = view(::RecyclerView, R.id.candidate_list) {
        layoutManager = candidateLytMgr
        adapter = candidateViewAdp
    }

    private var keyboards: HashMap<String, BaseKeyboard> = hashMapOf(
        "qwerty" to TextKeyboard(context, fcitx) { v, it, long ->
            onAction(v, it, long)
        },
        "t9" to NumberKeyboard(context, fcitx) { v, it, long ->
            onAction(v, it, long)
        },
    )
    private var currentKeyboard = "qwerty"

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
        add(keyboards[currentKeyboard]!!, lParams(matchParent, wrapContent))
    }

    fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                updateCandidates(it.data)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                cachedPreedit.aux = it.data
                updatePreedit(cachedPreedit)
            }
            is FcitxEvent.PreeditEvent -> it.data.let {
                cachedPreedit.preedit = it
                updatePreedit(cachedPreedit)
            }
            else -> {}
        }
    }

    private fun onAction(v: View, it: KeyAction<*>, long: Boolean) {
        when (it) {
            is KeyAction.LayoutSwitchAction -> switchLayout()
            else -> {}
        }
        passAction(v, it, long)
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

    private fun switchLayout() {
        removeView(keyboards[currentKeyboard])
        currentKeyboard = when (currentKeyboard) {
            "qwerty" -> "t9"
            else -> "qwerty"
        }
        add(keyboards[currentKeyboard]!!, lParams(matchParent, wrapContent))
    }

}
