package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent

class FcitxService : InputMethodService(), LifecycleOwner {
    private lateinit var fcitx: Fcitx
    private var fcitxReady = false
    private val dispatcher = ServiceLifecycleDispatcher(this)
    private val candidateLytMgr = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    private val candidateViewAdp = CandidateViewAdapter()
    private lateinit var preeditStartView: TextView
    private lateinit var preeditEndView: TextView
    private var preeditContent = object {
        var preedit = FcitxEvent.PreeditEvent.Data("", "")
            set(value) {
                field = value
                updatePreedit()
            }
        var aux = FcitxEvent.InputPanelAuxEvent.Data("", "")
            set(value) {
                field = value
                updatePreedit()
            }
    }
    private lateinit var capsButton: Button
    private lateinit var langSwitchButton: Button

    enum class CapsState { None, Once, Lock }

    private var capsState = CapsState.None

    override fun getLifecycle(): Lifecycle {
        return dispatcher.lifecycle
    }

    fun updatePreedit() {
        val start = preeditContent.aux.auxUp + preeditContent.preedit.preedit
        val end = preeditContent.aux.auxDown
        val hasStart = start.isNotEmpty()
        val hasEnd = end.isNotEmpty()
        setCandidatesViewShown(hasStart or hasEnd)
        preeditStartView.visibility = if (hasStart) View.VISIBLE else View.GONE
        preeditEndView.visibility = if (hasEnd) View.VISIBLE else View.GONE
        preeditStartView.text = start
        preeditEndView.text = end
    }

    override fun onCreate() {
        fcitx = Fcitx(this)
        lifecycle.addObserver(fcitx)
        fcitx.eventFlow.onEach {
            when (it) {
                is FcitxEvent.ReadyEvent -> {
                    fcitxReady = true
                    langSwitchButton.text = fcitx.imeStatus().label
                }
                is FcitxEvent.CommitStringEvent -> {
                    currentInputConnection?.commitText(it.data, 1)
                }
                is FcitxEvent.KeyEvent -> {
                    if (Character.isISOControl(it.data.code)) {
                        when (it.data.code) {
                            '\b'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                            '\r'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                            else -> Log.d("KeyEvent", "sym: `${it.data.sym}`; code: `${it.data.code}`")
                        }
                    } else {
                        sendKeyChar(Char(it.data.code))
                    }
                }
                is FcitxEvent.CandidateListEvent -> {
                    candidateViewAdp.candidates = it.data
                    candidateViewAdp.notifyDataSetChanged()
                    candidateLytMgr.scrollToPosition(0)
                }
                is FcitxEvent.InputPanelAuxEvent -> {
                    preeditContent.aux = it.data
                }
                is FcitxEvent.PreeditEvent -> {
                    preeditContent.preedit = it.data
                    currentInputConnection?.setComposingText(it.data.clientPreedit, 1)
                }
                is FcitxEvent.IMChangeEvent -> {
                    langSwitchButton.text = it.data.status.label
                }
                else -> {
                }
            }
        }.launchIn(lifecycle.coroutineScope)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    fun onButtonPress(v: View) {
        var c = (v as Button).text[0];
        when (capsState) {
            CapsState.None -> {
                c = c.lowercaseChar()
            }
            CapsState.Once -> {
                c = c.uppercaseChar()
                capsState = CapsState.None
            }
            CapsState.Lock -> {
                c = c.uppercaseChar()
            }
        }
        fcitx.sendKey(c)
    }

    fun onCapsPress(v: View) {
        capsState = when (capsState) {
            CapsState.None -> CapsState.Once
            CapsState.Once -> CapsState.Lock
            CapsState.Lock -> CapsState.None
        }
    }

    fun onBackspacePress(v: View) {
        fcitx.sendKey("BackSpace")
    }

    fun onLangSwitchPress(v: View) {
        if (!fcitxReady) return
        val list = fcitx.listIme()
        val status = fcitx.imeStatus()
        val index = list.indexOfFirst { it.uniqueName == status.uniqueName }
        val next = list[(index + 1) % list.size]
        fcitx.setIme(next.uniqueName)
    }

    private fun onLangSwitchLongPress(): Boolean {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        return true
    }

    fun onSpacePress(v: View) {
        fcitx.sendKey("space")
    }

    fun onEnterPress(v: View) {
        fcitx.sendKey("Return")
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.qwerty_keyboard, null)
        view.findViewById<RecyclerView>(R.id.candidate_list).also {
            it.layoutManager = candidateLytMgr
            candidateViewAdp.onSelectCallback = { idx -> fcitx.select(idx) }
            it.adapter = candidateViewAdp
        }
        view.findViewById<Button>(R.id.button_caps).also {
            capsButton = it
        }
        view.findViewById<Button>(R.id.button_lang).also {
            langSwitchButton = it
            it.setOnLongClickListener { this.onLangSwitchLongPress() }
        }
        return view
    }

    override fun onCreateCandidatesView(): View {
        layoutInflater.inflate(R.layout.keyboard_preedit, null).also {
            preeditStartView = it.findViewById(R.id.keyboard_preedit_text)
            preeditEndView = it.findViewById(R.id.keyboard_preedit_after_text)
            return it
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        val msg = fcitx.imeStatus()?.label ?: "×"
    }

    override fun onFinishInput() {
        fcitx.reset()
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
