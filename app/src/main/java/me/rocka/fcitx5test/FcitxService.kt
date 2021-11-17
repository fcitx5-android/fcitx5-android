package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
    private val dispatcher = ServiceLifecycleDispatcher(this)
    private val candidateLytMgr = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    private val candidateViewAdp = CandidateViewAdapter()
    private var setPreedit: (String) -> Unit = {}

    override fun getLifecycle(): Lifecycle {
        return dispatcher.lifecycle
    }

    override fun onCreate() {
        fcitx = Fcitx(this)
        lifecycle.addObserver(fcitx)
        fcitx.eventFlow.onEach {
            when (it) {
                is FcitxEvent.ReadyEvent -> {
                    Toast.makeText(this, "READY", Toast.LENGTH_SHORT).show()
                }
                is FcitxEvent.CommitStringEvent -> {
                    currentInputConnection.commitText(it.data, 1)
                }
                is FcitxEvent.KeyEvent -> {
                    if (Character.isISOControl(it.data.code)) {
                        when (it.data.code) {
                            '\b'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                            '\r'.code -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                            else -> Log.d("KeyEvent", "sym: `${it.data.sym}`; code: `${it.data.code}`")
                        }
                    } else {
                        currentInputConnection.commitText(it.data.sym, 1)
                    }
                }
                is FcitxEvent.CandidateListEvent -> {
                    candidateViewAdp.candidates = it.data
                    candidateViewAdp.notifyDataSetChanged()
                }
                is FcitxEvent.InputPanelAuxEvent -> {
                    val text = "${it.data.auxUp}\n${it.data.auxDown}"
                    if (text.length > 1) Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                }
                is FcitxEvent.PreeditEvent -> {
                    if (it.data.preedit.isEmpty()) {
                        setCandidatesViewShown(false)
                    } else {
                        setCandidatesViewShown(true)
                        setPreedit(it.data.preedit)
                    }
                    currentInputConnection.setComposingText(it.data.clientPreedit, 1)
                }
                else -> {
                }
            }
        }.launchIn(lifecycle.coroutineScope)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    fun onButtonPress(v: View) {
        fcitx.sendKey((v as Button).text.last().lowercaseChar())
    }

    fun onCapsPress(v: View) {
        fcitx.sendKey("Caps_Lock")
    }

    fun onBackspacePress(v: View) {
        fcitx.sendKey("BackSpace")
    }

    fun onLangSwitchPress(v: View) {
        val list = fcitx.listIme()
        val status = fcitx.imeStatus()
        val index = list.indexOfFirst { it -> it.uniqueName == status.uniqueName }
        val next = (index + 1) % list.size
        fcitx.setIme(list[next].uniqueName)
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
        view.findViewById<Button>(R.id.button_lang).also {
            it.setOnLongClickListener { this.onLangSwitchLongPress() }
        }
        return view
    }

    override fun onCreateCandidatesView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_preedit, null)
        val text = view.findViewById<TextView>(R.id.keyboard_preedit_text)
        setPreedit = { text.text = it }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        val msg = fcitx.imeStatus()?.label ?: "(Not Available)"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onFinishInput() {
        fcitx.reset()
    }
}
