package me.rocka.fcitx5test

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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
                    Toast.makeText(this, "${it.data.auxUp}\n${it.data.auxDown}", Toast.LENGTH_SHORT).show()
                }
                is FcitxEvent.UnknownEvent -> {
                    Log.i(javaClass.name, "unknown event: ${it.data}")
                }
            }
        }.launchIn(uiScope)
    }

    override fun onResume() {
        super.onResume()
        fcitx.sendKey("Escape")
        uiScope.launch {
            listOf("nihaoshijie", "shijienihao").forEach { str ->
                delay(2000)
                str.forEach { c ->
                    fcitx.sendKey(c)
                    delay(200)
                }
                delay(500)
                fcitx.select(0)
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
            Toast.makeText(this, "${fcitx.empty()}", Toast.LENGTH_SHORT).show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
