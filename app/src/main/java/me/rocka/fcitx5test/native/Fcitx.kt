package me.rocka.fcitx5test.native

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.rocka.fcitx5test.copyFileOrDir
import java.util.concurrent.atomic.AtomicBoolean

class Fcitx(private val context: Context) : DefaultLifecycleObserver {

    interface RawConfigMap {
        operator fun get(key: String): RawConfig?
        operator fun set(key: String, value: RawConfig)
    }

    private var fcitxJob: Job? = null

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    val eventFlow = eventFlow_.asSharedFlow()

    fun saveConfig() = saveFcitxConfig()
    fun sendKey(key: String) = sendKeyToFcitxString(key)
    fun sendKey(c: Char) = sendKeyToFcitxChar(c)
    fun select(idx: Int) = selectCandidate(idx)
    fun isEmpty() = isInputPanelEmpty()
    fun reset() = resetInputPanel()
    fun listIme() = listInputMethods()
    fun imeStatus() = inputMethodStatus()
    fun setIme(ime: String) = setInputMethod(ime)
    fun availableIme() = availableInputMethods()
    fun setEnabledIme(array: Array<String>) = setEnabledInputMethods(array)
    var globalConfig: RawConfig
        get() = getFcitxGlobalConfig()
        set(value) = setFcitxGlobalConfig(value)
    var addonConfig = object : RawConfigMap {
        override operator fun get(key: String) = getFcitxAddonConfig(key)
        override operator fun set(key: String, value: RawConfig) = setFcitxAddonConfig(key, value)
    }
    var imConfig = object : RawConfigMap {
        override operator fun get(key: String) = getFcitxInputMethodConfig(key)
        override operator fun set(key: String, value: RawConfig) =
            setFcitxInputMethodConfig(key, value)
    }
    fun addons() = getFcitxAddons()
    fun setAddonState(name: Array<String>, state: BooleanArray) = setFcitxAddonState(name, state)
    fun triggerQuickPhrase() = triggerQuickPhraseInput()

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
        external fun sendKeyToFcitxString(key: String)

        @JvmStatic
        external fun sendKeyToFcitxChar(c: Char)

        @JvmStatic
        external fun selectCandidate(idx: Int)

        @JvmStatic
        external fun isInputPanelEmpty(): Boolean

        @JvmStatic
        external fun resetInputPanel()

        @JvmStatic
        external fun listInputMethods(): Array<InputMethodEntry>

        @JvmStatic
        external fun inputMethodStatus(): InputMethodEntry

        @JvmStatic
        external fun setInputMethod(ime: String)

        @JvmStatic
        external fun availableInputMethods(): Array<InputMethodEntry>

        @JvmStatic
        external fun setEnabledInputMethods(array: Array<String>)

        @JvmStatic
        external fun getFcitxGlobalConfig(): RawConfig

        @JvmStatic
        external fun getFcitxAddonConfig(addon: String): RawConfig?

        @JvmStatic
        external fun getFcitxInputMethodConfig(im: String): RawConfig?

        @JvmStatic
        external fun setFcitxGlobalConfig(config: RawConfig)

        @JvmStatic
        external fun setFcitxAddonConfig(addon: String, config: RawConfig)

        @JvmStatic
        external fun setFcitxInputMethodConfig(im: String, config: RawConfig)

        @JvmStatic
        external fun getFcitxAddons(): Array<AddonInfo>

        @JvmStatic
        external fun setFcitxAddonState(name: Array<String>, state: BooleanArray)

        @JvmStatic
        external fun triggerQuickPhraseInput()

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
            eventFlow_.tryEmit(FcitxEvent.create(type, params.asList()))
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        with(context) {
            fcitxJob = launch {
                copyFileOrDir("fcitx5")
                val externalFilesDir = getExternalFilesDir(null)!!
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