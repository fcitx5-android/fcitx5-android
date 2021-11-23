package me.rocka.fcitx5test

import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import me.rocka.fcitx5test.databinding.KeyboardPreeditBinding
import me.rocka.fcitx5test.databinding.QwertyKeyboardBinding
import me.rocka.fcitx5test.native.InputMethodEntry
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


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

    private val config:VKconfig = readVirtualKeyboardConfig()

    //TODO 这也太丑了，还有什么好办法从配置给出的 Button 名字获取到 Button View 吗， FindbyId 性能有问题吗
    private val lMap : Map<String,View> = mapOf<String,View>(
        "l0" to keyboardBinding.l0.l0root,
        "l1" to keyboardBinding.l1.l1root,
        "l2" to keyboardBinding.l2.l2root
    )
    private val bMap : Map<String, View> = mapOf<String,View>(
        "l0r0k0" to keyboardBinding.l0.l0r0k0,
        "l0r0k1" to keyboardBinding.l0.l0r0k1,
        "l0r0k2" to keyboardBinding.l0.l0r0k2,
        "l0r0k3" to keyboardBinding.l0.l0r0k3,
        "l0r0k4" to keyboardBinding.l0.l0r0k4,
        "l0r0k5" to keyboardBinding.l0.l0r0k5,
        "l0r0k6" to keyboardBinding.l0.l0r0k6,
        "l0r0k7" to keyboardBinding.l0.l0r0k7,
        "l0r0k8" to keyboardBinding.l0.l0r0k8,
        "l0r0k9" to keyboardBinding.l0.l0r0k9,
        "l0r1k0" to keyboardBinding.l0.l0r1k0,
        "l0r1k1" to keyboardBinding.l0.l0r1k1,
        "l0r1k2" to keyboardBinding.l0.l0r1k2,
        "l0r1k3" to keyboardBinding.l0.l0r1k3,
        "l0r1k4" to keyboardBinding.l0.l0r1k4,
        "l0r1k5" to keyboardBinding.l0.l0r1k5,
        "l0r1k6" to keyboardBinding.l0.l0r1k6,
        "l0r1k7" to keyboardBinding.l0.l0r1k7,
        "l0r1k8" to keyboardBinding.l0.l0r1k8,
        "l0r2k0" to keyboardBinding.l0.l0r2k0,
        "l0r2k1" to keyboardBinding.l0.l0r2k1,
        "l0r2k2" to keyboardBinding.l0.l0r2k2,
        "l0r2k3" to keyboardBinding.l0.l0r2k3,
        "l0r2k4" to keyboardBinding.l0.l0r2k4,
        "l0r2k5" to keyboardBinding.l0.l0r2k5,
        "l0r2k6" to keyboardBinding.l0.l0r2k6,
        "l0r2k7" to keyboardBinding.l0.l0r2k7,
        "l0r2k8" to keyboardBinding.l0.l0r2k8,
        "l0r3k0" to keyboardBinding.l0.l0r3k0,
        "l0r3k1" to keyboardBinding.l0.l0r3k1,
        "l0r3k2" to keyboardBinding.l0.l0r3k2,
        "l0r3k3" to keyboardBinding.l0.l0r3k3,
        "l0r3k4" to keyboardBinding.l0.l0r3k4,
        "l0r3k5" to keyboardBinding.l0.l0r3k5,
        "l1r0k0" to keyboardBinding.l1.l1r0k0,
        "l1r0k1" to keyboardBinding.l1.l1r0k1,
        "l1r0k2" to keyboardBinding.l1.l1r0k2,
        "l1r0k3" to keyboardBinding.l1.l1r0k3,
        "l1r0k4" to keyboardBinding.l1.l1r0k4,
        "l1r0k5" to keyboardBinding.l1.l1r0k5,
        "l1r0k6" to keyboardBinding.l1.l1r0k6,
        "l1r0k7" to keyboardBinding.l1.l1r0k7,
        "l1r0k8" to keyboardBinding.l1.l1r0k8,
        "l1r0k9" to keyboardBinding.l1.l1r0k9,
        "l1r1k0" to keyboardBinding.l1.l1r1k0,
        "l1r1k1" to keyboardBinding.l1.l1r1k1,
        "l1r1k2" to keyboardBinding.l1.l1r1k2,
        "l1r1k3" to keyboardBinding.l1.l1r1k3,
        "l1r1k4" to keyboardBinding.l1.l1r1k4,
        "l1r1k5" to keyboardBinding.l1.l1r1k5,
        "l1r1k6" to keyboardBinding.l1.l1r1k6,
        "l1r1k7" to keyboardBinding.l1.l1r1k7,
        "l1r1k8" to keyboardBinding.l1.l1r1k8,
        "l1r1k9" to keyboardBinding.l1.l1r1k9,
        "l1r2k0" to keyboardBinding.l1.l1r2k0,
        "l1r2k1" to keyboardBinding.l1.l1r2k1,
        "l1r2k2" to keyboardBinding.l1.l1r2k2,
        "l1r2k3" to keyboardBinding.l1.l1r2k3,
        "l1r2k4" to keyboardBinding.l1.l1r2k4,
        "l1r2k5" to keyboardBinding.l1.l1r2k5,
        "l1r2k6" to keyboardBinding.l1.l1r2k6,
        "l1r2k7" to keyboardBinding.l1.l1r2k7,
        "l1r2k8" to keyboardBinding.l1.l1r2k8,
        "l1r3k0" to keyboardBinding.l1.l1r3k0,
        "l1r3k1" to keyboardBinding.l1.l1r3k1,
        "l1r3k2" to keyboardBinding.l1.l1r3k2,
        "l1r3k3" to keyboardBinding.l1.l1r3k3,
        "l1r3k4" to keyboardBinding.l1.l1r3k4,
        "l1r3k5" to keyboardBinding.l1.l1r3k5,
        "l2r0k0" to keyboardBinding.l2.l2r0k0,
        "l2r0k1" to keyboardBinding.l2.l2r0k1,
        "l2r0k2" to keyboardBinding.l2.l2r0k2,
        "l2r0k3" to keyboardBinding.l2.l2r0k3,
        "l2r0k4" to keyboardBinding.l2.l2r0k4,
        "l2r1k0" to keyboardBinding.l2.l2r1k0,
        "l2r1k1" to keyboardBinding.l2.l2r1k1,
        "l2r1k2" to keyboardBinding.l2.l2r1k2,
        "l2r1k3" to keyboardBinding.l2.l2r1k3,
        "l2r1k4" to keyboardBinding.l2.l2r1k4,
        "l2r2k0" to keyboardBinding.l2.l2r2k0,
        "l2r2k1" to keyboardBinding.l2.l2r2k1,
        "l2r2k2" to keyboardBinding.l2.l2r2k2,
        "l2r2k3" to keyboardBinding.l2.l2r2k3,
        "l2r2k4" to keyboardBinding.l2.l2r2k4,
        "l2r3k0" to keyboardBinding.l2.l2r3k0,
        "l2r3k1" to keyboardBinding.l2.l2r3k1,
        "l2r3k2" to keyboardBinding.l2.l2r3k2,
        "l2r3k3" to keyboardBinding.l2.l2r3k3,
        "l2r3k4" to keyboardBinding.l2.l2r3k4,
        "l2r3k5" to keyboardBinding.l2.l2r3k5,
        "l2r3k6" to keyboardBinding.l2.l2r3k6,
    )

    init {
        with(keyboardBinding) {
            candidateList.let {
                it.layoutManager = candidateLytMgr
                candidateViewAdp.onSelectCallback = { idx -> presenter.selectCandidate(idx) }
                it.adapter = candidateViewAdp
            }
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

    override fun updateVirtualKeyboard(entry: InputMethodEntry) {
        var flag:Int = 0
        for (ime in config.imes)
            if (ime.name == entry.name){
                changevKeyboard(ime.vKeyboard, entry.name)
                flag = 1
                break
            }

        if (flag == 0)
            changevKeyboard(config.vKeyboards[0].name, entry.name)
    }

    private fun changevKeyboard(vKeyboard_name : String,lang_name: String) {
        for (v in config.vKeyboards)
            if (v.name == vKeyboard_name){
                lMap.forEach{ (lName, lView) ->
                    if (lName == v.layout)
                        lView.visibility = View.VISIBLE
                    else
                        lView.visibility =View.GONE
                }

                for (key in v.keys){
                    bMap[key.name]?.let{
                        when(it){
                            is Button -> it.text = if (key.display == "lang") lang_name else key.display
                            //TODO 对于 ImageButton 修改内容
                            //is ImageButton -> it.setImageResource(R.drawable.ic_baseline_backspace_24)
                            else -> {}
                        }

                         when(key.short.type){
                            "fcitx" -> it.setOnClickListener{ presenter.onKeyPress(key.short.text[0]) }
                            "nofcitx" -> it.setOnClickListener{presenter.onNoFcitxKeyPress(key.short.text)}
                            "backspace" -> {
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
                            "caps" -> it.setOnClickListener { presenter.switchCapsState() }
                            "lang" -> {
                                    it.setOnClickListener { presenter.switchLang() }
                                    it.setOnLongClickListener {
                                        (service.getSystemService(InputMethodService.INPUT_METHOD_SERVICE)
                                                as InputMethodManager).showInputMethodPicker()
                                        true
                                    }

                            }
                            "quickphrase" -> it.setOnClickListener { presenter.quickPhrase() }
                            "space" -> it.setOnClickListener { presenter.space() }
                            "enter" -> it.setOnClickListener { presenter.enter() }
                            "switch" -> it.setOnClickListener{ changevKeyboard(key.short.text,lang_name) }
                            else -> {}
                        }

                        when(key.long.type){
                            "fcitx" -> it.setOnLongClickListener{
                                presenter.onKeyPress(key.long.text[0])
                                true}
                            "nofcitx" -> it.setOnLongClickListener{
                                presenter.onNoFcitxKeyPress(key.long.text)
                                true}
                            "quickphrase" -> it.setOnClickListener { presenter.quickPhrase() }

                            else -> {}
                        }
                    }

                }
                break
            }
    }

    private fun readVirtualKeyboardConfig(): VKconfig {
        var config: VKconfig
        val gson = Gson()

        val configFile: File = File(service.getExternalFilesDir("config"), "keyboard_config.json")
        val sampleConfig:String = BufferedReader(InputStreamReader( service.assets.open("sampleconfig.json"))).readText()

        if (configFile.exists()) {
            try {
                config = gson.fromJson(configFile.readText(), VKconfig::class.java)
                return config
            } catch (e: JsonSyntaxException) {
                val toast = Toast.makeText(service, "配置文件格式错误使用预设配置", Toast.LENGTH_SHORT)
                toast.show()
            }
        } else {
            configFile.writeText(sampleConfig)
            val toast = Toast.makeText(service, "配置文件不存在已写入预设配置", Toast.LENGTH_SHORT)
            toast.show()
        }
        config = gson.fromJson(sampleConfig,VKconfig::class.java)
        return  config
    }

}