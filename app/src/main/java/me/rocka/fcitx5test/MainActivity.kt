package me.rocka.fcitx5test

import android.app.Activity
import android.os.Bundle
import android.util.Log
import me.rocka.fcitx5test.databinding.ActivityMainBinding
import me.rocka.fcitx5test.native.FcitxEvent
import me.rocka.fcitx5test.native.JNI
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        copyFileOrDir("fcitx5")
        Log.i(
            javaClass.name,
            File("${applicationInfo.dataDir}/fcitx5")
                .listFiles()?.joinToString()
                ?: "No data found"
        )
        thread {
            JNI.startupFcitx(
                applicationInfo.dataDir,
                applicationInfo.nativeLibraryDir,
                getExternalFilesDir(null)!!.absolutePath
            )
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        thread {
            listOf("nihaoshijie", "shijienihao").forEach { str ->
                Thread.sleep(2000)
                str.forEach { c ->
                    JNI.sendKeyToFcitx(c)
                    Thread.sleep(200)
                }
                Thread.sleep(500)
                JNI.selectCandidate(0)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CandidateListEvent -> {
                binding.candidate.text = event.data.joinToString(separator = " | ")
            }
            is FcitxEvent.CommitStringEvent -> {
                binding.commit.text = event.data
            }
            is FcitxEvent.PreeditEvent -> {
                binding.input.text = "${event.data.clientPreedit}\n${event.data.preedit}"
            }
            is FcitxEvent.UnknownEvent -> {
                Log.i(javaClass.name, "unknown event: ${event.data}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        JNI.exitFcitx()
    }

}
