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
                getExternalFilesDir(null)!!.absolutePath + "/config",
                "${applicationInfo.dataDir}/fcitx5/libime"
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
            Thread.sleep(1000)
            "nihaoshijie".forEach {
                JNI.sendKeyToFcitx(it)
                Thread.sleep(200)
            }
            Thread.sleep(500)
            JNI.selectCandidate(0)

            Thread.sleep(2000)
            "shijienihao".forEach {
                JNI.sendKeyToFcitx(it)
                Thread.sleep(200)
            }
            Thread.sleep(500)
            JNI.selectCandidate(0)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CandidateListEvent -> {
                Log.i(javaClass.name, "candidate update: ${event.data}")
                binding.candidate.text = event.data.joinToString(separator = " | ")
            }
            is FcitxEvent.CommitStringEvent -> {
                Log.i(javaClass.name, "commit update: ${event.data}")
                binding.commit.text = event.data
            }
            is FcitxEvent.PreeditEvent -> {
                Log.i(javaClass.name, "preedit update: ${event.data}")
                binding.input.text = event.data.preedit
            }
            is FcitxEvent.UnknownEvent -> {
                Log.i(javaClass.name, "unknown event: ${event.data}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

}
