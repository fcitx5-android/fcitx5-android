package me.rocka.fcitx5test

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.databinding.ActivityMainBinding
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fcitx: Fcitx
    private val uiScope
        get() = lifecycle.coroutineScope

    private fun toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, msg, duration).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        fcitx = Fcitx(this)
        lifecycle.addObserver(fcitx)
        fcitx.eventFlow.onEach {
            when (it) {
                is FcitxEvent.CandidateListEvent -> {
                    binding.candidate.text = it.data.joinToString(separator = " | ")
                }
                is FcitxEvent.CommitStringEvent -> {
                    binding.commit.text = it.data
                }
                is FcitxEvent.PreeditEvent -> {
                    binding.input.text = "${it.data.clientPreedit}\n${it.data.preedit}"
                }
                is FcitxEvent.InputPanelAuxEvent -> {
                    val text = "${it.data.auxUp}\n${it.data.auxDown}"
                    if (text.length > 1) toast(text)
                }
                is FcitxEvent.UnknownEvent -> {
                    Log.i(javaClass.name, "unknown event: ${it.data}")
                }
            }
        }.launchIn(uiScope)
    }

    override fun onResume() {
        super.onResume()
        uiScope.launch {
            delay(2000)
            val keySeq = with(fcitx.imeStatus()) {
                when {
                    startsWith("pinyin") -> listOf("nihaoshijie", "shijienihao")
                    startsWith("shuangpin") -> listOf("nihkuijx", "uijxnihk")
                    startsWith("wb") -> listOf("wqvbanlw", "anlwwqvb")
                    else -> listOf("")
                }
            }
            keySeq.forEach { str ->
                str.forEach { c ->
                    fcitx.sendKey(c)
                    delay(200)
                }
                delay(500)
                fcitx.select(0)
                fcitx.reset()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main_actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.activity_main_reset -> {
            fcitx.reset()
            true
        }
        R.id.activity_main_empty -> {
            toast("${fcitx.empty()}")
            true
        }
        R.id.activity_main_get_available -> {
            val list = fcitx.listIme()
            val status = fcitx.imeStatus()
            val current = list.indexOfFirst { status.startsWith(it) }
            AlertDialog.Builder(this)
                .setTitle("Change IME")
                .setSingleChoiceItems(list, current) { dialog, choice ->
                    val ime = list[choice]
                    fcitx.setIme(ime.split(":")[0])
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
            true
        }
        R.id.activity_main_set_enabled -> {
            val enabled = fcitx.listIme()
            val available = fcitx.availableIme()
            val strAvail = available.map { "${it.uniqueName}:${it.name}" } .toTypedArray()
            val boolAvail = available.map { avail ->
                enabled.indexOfFirst { it.startsWith(avail.uniqueName) } >= 0
            } .toBooleanArray()
            AlertDialog.Builder(this)
                .setTitle("Enabled IME")
                .setMultiChoiceItems(strAvail, boolAvail) { _, which, checked ->
                    boolAvail[which] = checked;
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("OK") { _, _ ->
                    fcitx.setEnabledIme(available.filterIndexed { i, _ -> boolAvail[i] } .map { it.uniqueName } .toTypedArray());
                }
                .show()
            true
        }
        R.id.activity_main_addons -> {
            val addons = fcitx.addons()
            val nameArray = addons.map { "${it.uniqueName}:${it.name}" } .toTypedArray()
            val stateArray = addons.map { it.enabled } .toBooleanArray()
            AlertDialog.Builder(this)
                .setTitle("Enabled Addons")
                .setMultiChoiceItems(nameArray, stateArray) { _, which, checked ->
                    stateArray[which] = checked
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("OK") { _, _ ->
                    fcitx.setAddonState(nameArray, stateArray)
                }
                .show()
            true
        }
        R.id.activity_main_save_config -> {
            fcitx.saveConfig()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
