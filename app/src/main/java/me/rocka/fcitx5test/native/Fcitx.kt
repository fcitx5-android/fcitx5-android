package me.rocka.fcitx5test.native

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.rocka.fcitx5test.AppSharedPreferences
import me.rocka.fcitx5test.BuildConfig
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.copyFileOrDir
import me.rocka.fcitx5test.native.FcitxState.*

class Fcitx(private val context: Context) : FcitxLifecycleOwner, CoroutineScope by CoroutineScope(
    SupervisorJob() + Dispatchers.IO
) {

    interface RawConfigMap {
        operator fun get(key: String): RawConfig
        operator fun set(key: String, value: RawConfig)
    }

    override val currentState: FcitxState
        get() = fcitxState_

    @Volatile
    override var observer: FcitxLifecycleObserver? = null
        set(value) {
            onStateChanged = {
                when (it) {
                    Starting to Ready -> value?.onReady()
                    Stopping to Stopped -> value?.onStopped()
                }
            }
            field = value
        }

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    val eventFlow = eventFlow_.asSharedFlow()

    fun saveConfig() = saveFcitxConfig()
    fun sendKey(key: String) = sendKeyToFcitxString(key)
    fun sendKey(c: Char) = sendKeyToFcitxChar(c)
    fun sendKey(i: Int) = sendKeyToFcitxInt(i)
    fun select(idx: Int) = selectCandidate(idx)
    fun isEmpty() = isInputPanelEmpty()
    fun reset() = resetInputPanel()
    fun moveCursor(position: Int) = repositionCursor(position)
    fun availableIme() = availableInputMethods() ?: arrayOf()
    fun enabledIme() = listInputMethods() ?: arrayOf()
    fun setEnabledIme(array: Array<String>) = setEnabledInputMethods(array)
    fun activateIme(ime: String) = setInputMethod(ime)
    fun ime() =
        inputMethodStatus() ?: InputMethodEntry(
            "",
            context.getString(R.string._not_available_),
            "",
            "",
            "×",
            "",
            false
        )

    var globalConfig: RawConfig
        get() = getFcitxGlobalConfig() ?: RawConfig(arrayOf())
        set(value) = setFcitxGlobalConfig(value)
    var addonConfig = object : RawConfigMap {
        override operator fun get(key: String) = getFcitxAddonConfig(key) ?: RawConfig(arrayOf())
        override operator fun set(key: String, value: RawConfig) = setFcitxAddonConfig(key, value)
    }
    var imConfig = object : RawConfigMap {
        override operator fun get(key: String) =
            getFcitxInputMethodConfig(key) ?: RawConfig(arrayOf())

        override operator fun set(key: String, value: RawConfig) =
            setFcitxInputMethodConfig(key, value)
    }

    fun addons() = getFcitxAddons() ?: arrayOf()
    fun setAddonState(name: Array<String>, state: BooleanArray) = setFcitxAddonState(name, state)
    fun triggerQuickPhrase() = triggerQuickPhraseInput()
    fun punctuation(c: Char, language: String = "zh_CN"): Pair<String, String> =
        queryPunctuation(c, language)?.let { Pair(it[0], it[1]) } ?: "$c".let { Pair(it, it) }

    fun triggerUnicode() = triggerUnicodeInput()

    init {
        if (fcitxState_ != Stopped)
            throw IllegalAccessException("Fcitx5 is already running!")
    }

    private companion object JNI {

        @Volatile
        private var fcitxState_ = Stopped
            set(value) {
                onStateChanged(field to value)
                field = value
            }

        private var onStateChanged: (Pair<FcitxState, FcitxState>) -> Unit = {}

        private val eventFlow_ =
            MutableSharedFlow<FcitxEvent<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

        private var firstRun = false

        init {
            System.loadLibrary("native-lib")
        }

        @JvmStatic
        external fun startupFcitx(
            locale: String,
            appData: String,
            appLib: String,
            extData: String
        ): Int

        @JvmStatic
        external fun exitFcitx()

        @JvmStatic
        external fun saveFcitxConfig()

        @JvmStatic
        external fun sendKeyToFcitxString(key: String)

        @JvmStatic
        external fun sendKeyToFcitxChar(c: Char)

        @JvmStatic
        external fun sendKeyToFcitxInt(i: Int)

        @JvmStatic
        external fun selectCandidate(idx: Int)

        @JvmStatic
        external fun isInputPanelEmpty(): Boolean

        @JvmStatic
        external fun resetInputPanel()

        @JvmStatic
        external fun repositionCursor(position: Int)

        @JvmStatic
        external fun listInputMethods(): Array<InputMethodEntry>?

        @JvmStatic
        external fun inputMethodStatus(): InputMethodEntry?

        @JvmStatic
        external fun setInputMethod(ime: String)

        @JvmStatic
        external fun availableInputMethods(): Array<InputMethodEntry>?

        @JvmStatic
        external fun setEnabledInputMethods(array: Array<String>)

        @JvmStatic
        external fun getFcitxGlobalConfig(): RawConfig?

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
        external fun getFcitxAddons(): Array<AddonInfo>?

        @JvmStatic
        external fun setFcitxAddonState(name: Array<String>, state: BooleanArray)

        @JvmStatic
        external fun triggerQuickPhraseInput()

        @JvmStatic
        external fun queryPunctuation(c: Char, language: String): Array<String>?

        @JvmStatic
        external fun triggerUnicodeInput()

        /**
         * Called from native-lib
         */
        @Suppress("unused")
        @JvmStatic
        fun handleFcitxEvent(type: Int, vararg params: Any) {
            val event = FcitxEvent.create(type, params.asList())
            Log.d(
                "FcitxEvent",
                "${event.eventType}[${params.size}]${params.take(10).joinToString()}"
            )
            if (event is FcitxEvent.ReadyEvent) {
                fcitxState_ = Ready
                if (firstRun) {
                    onFirstRun()
                }
            }
            eventFlow_.tryEmit(event)
        }

        private fun onFirstRun() {
            Log.i("Fcitx", "onFirstRun")
            getFcitxGlobalConfig()?.get("cfg")?.run {
                get("Behavior")["PreeditEnabledByDefault"].value = "False"
                setFcitxGlobalConfig(this)
            }
            getFcitxAddonConfig("pinyin")?.get("cfg")?.run {
                get("PreeditInApplication").value = "False"
                get("PreeditCursorPositionAtBeginning").value = "False"
                setFcitxAddonConfig("pinyin", this)
            }
        }
    }

    override fun start() {
        if (fcitxState_ != Stopped)
            return
        fcitxState_ = Starting
        with(context) {
            launch {
                val ver = AppSharedPreferences.getInstance().assetsVersion

                if (ver == -1L) {
                    firstRun = true
                }

                if (BuildConfig.ASSETS_VERSION > ver) {
                    copyFileOrDir("usr")
                    AppSharedPreferences.getInstance().assetsVersion = BuildConfig.ASSETS_VERSION
                }

                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resources.configuration.locales[0].run { "${language}_${country}" }
                } else {
                    @Suppress("DEPRECATION")
                    resources.configuration.locale.run { "${language}_${country}" }
                }
                val externalFilesDir = getExternalFilesDir(null)!!
                startupFcitx(
                    locale,
                    applicationInfo.dataDir,
                    applicationInfo.nativeLibraryDir,
                    externalFilesDir.absolutePath
                )
            }
        }
    }

    override fun stop() {
        if (fcitxState_ != Ready)
            return
        fcitxState_ = Stopping
        exitFcitx()
        coroutineContext.cancel()
        fcitxState_ = Stopped
    }

}