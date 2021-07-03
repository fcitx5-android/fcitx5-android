package me.rocka.fcitx5test.native

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.rocka.fcitx5test.copyFileOrDir
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class Fcitx(private val context: Context) : DefaultLifecycleObserver {

    private var fcitxJob: Job? = null

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    val eventFlow = eventFlow_.asSharedFlow()

    fun saveConfig() = saveFcitxConfig()
    fun sendKey(key: String) = sendKeyToFcitx(key)
    fun sendKey(c: Char) = sendKeyToFcitx(c)
    fun select(idx: Int) = selectCandidate(idx)
    fun empty() = isInputPanelEmpty()
    fun reset() = resetInputPanel()
    fun listIme() = listInputMethods()
    fun imeStatus() = inputMethodStatus()
    fun setIme(ime: String) = setInputMethod(ime)

    init {
        if (isRunning.get())
            throw IllegalAccessException("Fcitx5 is already running!")
    }

    private companion object JNI: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
        private var isRunning = AtomicBoolean(false)

        private val eventFlow_ =
            MutableSharedFlow<FcitxEvent<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )


        init {
            System.loadLibrary("native-lib")
        }

        @JvmStatic
        external fun startupFcitx(appData: String, appLib: String, extData: String): Int

        @JvmStatic
        external fun exitFcitx()

        @JvmStatic
        external fun saveFcitxConfig()

        @JvmStatic
        @JvmName("sendKeyToFcitxString")
        external fun sendKeyToFcitx(key: String)

        @JvmStatic
        @JvmName("sendKeyToFcitxChar")
        external fun sendKeyToFcitx(c: Char)

        @JvmStatic
        external fun selectCandidate(idx: Int)

        @JvmStatic
        external fun isInputPanelEmpty(): Boolean

        @JvmStatic
        external fun resetInputPanel()

        @JvmStatic
        external fun listInputMethods(): Array<String>

        @JvmStatic
        external fun inputMethodStatus(): String

        @JvmStatic
        external fun setInputMethod(ime: String)

        /**
         * Called from native-lib
         */
        @Suppress("unused")
        @JvmStatic
        fun handleFcitxEvent(type: Int, vararg params: Any) {
            Log.d(
                "FcitxEvent",
                "type=${type}, params=${params.run { "[$size]" + joinToString(",") }}"
            )
            launch {
                eventFlow_.emit(FcitxEvent.create(type, params.asList()))
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        with(context) {
            fcitxJob = launch {
                copyFileOrDir("fcitx5")
                val externalFilesDir = getExternalFilesDir(null)!!
                File(externalFilesDir.absolutePath, "data").mkdirs()
                File(externalFilesDir.absolutePath, "config").mkdirs()
                // TODO: should be set in a callback which indicates fcitx has started
                isRunning.set(true)
                startupFcitx(
                    applicationInfo.dataDir,
                    applicationInfo.nativeLibraryDir,
                    externalFilesDir.absolutePath
                )
                isRunning.set(false)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        exitFcitx()
        runBlocking {
            fcitxJob?.cancelAndJoin()
        }
        fcitxJob = null
    }

}