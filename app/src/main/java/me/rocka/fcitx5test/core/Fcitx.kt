package me.rocka.fcitx5test.core

import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.DataManager
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.data.clipboard.ClipboardManager
import splitties.resources.str
import timber.log.Timber

class Fcitx(private val context: Context) : FcitxLifecycleOwner by JNI {

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    val eventFlow = eventFlow_.asSharedFlow()

    val isReady
        get() = lifecycle.currentState == FcitxLifecycle.State.READY

    suspend fun save() = dispatcher.dispatch { saveFcitxState() }
    suspend fun sendKey(key: String) = dispatcher.dispatch { sendKeyToFcitxString(key) }
    suspend fun sendKey(c: Char) = dispatcher.dispatch { sendKeyToFcitxChar(c) }
    suspend fun sendKey(i: Int) = dispatcher.dispatch { sendKeyToFcitxInt(i) }
    suspend fun select(idx: Int) = dispatcher.dispatch { selectCandidate(idx) }
    suspend fun isEmpty() = dispatcher.dispatch { isInputPanelEmpty() }
    suspend fun reset() = dispatcher.dispatch { resetInputContext() }
    suspend fun moveCursor(position: Int) = dispatcher.dispatch { repositionCursor(position) }
    suspend fun availableIme() =
        dispatcher.dispatch { availableInputMethods() ?: arrayOf() }

    suspend fun enabledIme() =
        dispatcher.dispatch { listInputMethods() ?: arrayOf() }

    suspend fun setEnabledIme(array: Array<String>) =
        dispatcher.dispatch { setEnabledInputMethods(array) }

    suspend fun activateIme(ime: String) = dispatcher.dispatch { setInputMethod(ime) }
    suspend fun enumerateIme(forward: Boolean = true) =
        dispatcher.dispatch { nextInputMethod(forward) }

    suspend fun currentIme() =
        dispatcher.dispatch {
            inputMethodStatus() ?: InputMethodEntry(context.str(R.string._not_available_))
        }

    suspend fun getGlobalConfig() = dispatcher.dispatch {
        getFcitxGlobalConfig() ?: RawConfig(arrayOf())
    }

    suspend fun setGlobalConfig(config: RawConfig) = dispatcher.dispatch {
        setFcitxGlobalConfig(config)
    }

    suspend fun getAddonConfig(key: String) = dispatcher.dispatch {
        getFcitxAddonConfig(key) ?: RawConfig(arrayOf())
    }

    suspend fun setAddonConfig(key: String, config: RawConfig) = dispatcher.dispatch {
        setFcitxAddonConfig(key, config)
    }

    suspend fun getImConfig(key: String) = dispatcher.dispatch {
        getFcitxInputMethodConfig(key) ?: RawConfig(arrayOf())
    }

    suspend fun setImConfig(key: String, config: RawConfig) = dispatcher.dispatch {
        setFcitxInputMethodConfig(key, config)
    }

    suspend fun addons() = dispatcher.dispatch { getFcitxAddons() ?: arrayOf() }
    suspend fun setAddonState(name: Array<String>, state: BooleanArray) =
        dispatcher.dispatch { setFcitxAddonState(name, state) }

    suspend fun triggerQuickPhrase() = dispatcher.dispatch { triggerQuickPhraseInput() }
    suspend fun punctuation(c: Char, language: String = "zh_CN"): Pair<String, String> =
        dispatcher.dispatch {
            queryPunctuation(c, language)?.let { it[0] to it[1] } ?: "$c".let { it to it }
        }

    suspend fun triggerUnicode() = dispatcher.dispatch { triggerUnicodeInput() }
    private suspend fun setClipboard(string: String) =
        dispatcher.dispatch { setFcitxClipboard(string) }

    suspend fun focus(focus: Boolean = true) = dispatcher.dispatch { focusInputContext(focus) }
    suspend fun setCapFlags(flags: CapabilityFlags) =
        dispatcher.dispatch { setCapabilityFlags(flags.toLong()) }

    suspend fun reloadPinyinDict() =
        dispatcher.dispatch { setAddonSubConfig("pinyin", "dictmanager", RawConfig(arrayOf())) }

    init {
        if (lifecycle.currentState != FcitxLifecycle.State.STOPPED)
            throw IllegalAccessException("Fcitx5 is already created!")
    }

    private companion object JNI : FcitxLifecycleOwner {

        private val lifecycleRegistry by lazy { FcitxLifecycleRegistry() }

        private val eventFlow_ =
            MutableSharedFlow<FcitxEvent<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

        init {
            System.loadLibrary("native-lib")
            NativeLib.instance.setup_log_stream {
                if (it.isEmpty()) return@setup_log_stream
                when (it.first()) {
                    'F' -> Timber.wtf(it.drop(1))
                    'D' -> Timber.d(it.drop(1))
                    'I' -> Timber.i(it.drop(1))
                    'W' -> Timber.w(it.drop(1))
                    'E' -> Timber.e(it.drop(1))
                    else -> Timber.d(it)
                }
            }
        }

        @JvmStatic
        external fun startupFcitx(
            locale: String,
            appData: String,
            appLib: String,
            extData: String
        )

        @JvmStatic
        external fun exitFcitx()

        @JvmStatic
        external fun saveFcitxState()

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
        external fun resetInputContext()

        @JvmStatic
        external fun repositionCursor(position: Int)

        @JvmStatic
        external fun nextInputMethod(forward: Boolean)

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

        @JvmStatic
        external fun setFcitxClipboard(string: String)

        @JvmStatic
        external fun focusInputContext(focus: Boolean)

        @JvmStatic
        external fun setCapabilityFlags(flags: Long)

        @JvmStatic
        external fun setAddonSubConfig(addon: String, path: String, config: RawConfig)

        @JvmStatic
        external fun loopOnce()

        @JvmStatic
        external fun scheduleEmpty()

        /**
         * Called from native-lib
         */
        @Suppress("unused")
        @JvmStatic
        fun handleFcitxEvent(type: Int, params: Array<Any>) {
            val event = FcitxEvent.create(type, params)
            Timber.d("Handling $event")
            if (event is FcitxEvent.ReadyEvent) {
                if (Prefs.getInstance().firstRun) {
                    // this method runs in same thread with `startupFcitx`
                    // block it will also block fcitx
                    onFirstRun()
                }
                onReady()
            }
            eventFlow_.tryEmit(event)
        }

        // will be called in fcitx main thread
        private fun onFirstRun() {
            Timber.i("onFirstRun")
            getFcitxGlobalConfig()?.get("cfg")?.run {
                get("Behavior")["PreeditEnabledByDefault"].value = "False"
                setFcitxGlobalConfig(this)
            }
            getFcitxAddonConfig("pinyin")?.get("cfg")?.run {
                get("PreeditInApplication").value = "False"
                get("PreeditCursorPositionAtBeginning").value = "False"
                setFcitxAddonConfig("pinyin", this)
            }
            Prefs.getInstance().firstRun = false
        }

        // will be called in fcitx main thread
        private fun onReady() {
            lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_READY)
            setCapabilityFlags(CapabilityFlags.DefaultFlags.toLong())
        }

        override val lifecycle: FcitxLifecycle
            get() = lifecycleRegistry
    }

    val dispatcher = FcitxDispatcher(object : FcitxDispatcher.FcitxController {
        override fun nativeStartup() {
            with(context) {
                DataManager.sync()
                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val locales = resources.configuration.locales
                    StringBuilder().apply {
                        for (i in 0 until locales.size()) {
                            if (i != 0) append(":")
                            append(locales[i].run { "${language}_${country}:$language" })
                            // since there is not an `en.mo` file, `en` must be the only locale
                            // in order to use default english translation
                            if (i == 0 && locales[i].language == "en") break
                        }
                    }.toString()
                } else {
                    @Suppress("DEPRECATION")
                    resources.configuration.locale.run { "${language}_${country}:$language" }
                }
                Timber.i("Current locale is $locale")
                val externalFilesDir = getExternalFilesDir(null)!!
                startupFcitx(
                    locale,
                    applicationInfo.dataDir,
                    applicationInfo.nativeLibraryDir,
                    externalFilesDir.absolutePath
                )
            }
        }

        override fun nativeLoopOnce() {
            loopOnce()
        }

        override fun nativeScheduleEmpty() {
            scheduleEmpty()
        }

        override fun nativeExit() {
            exitFcitx()
        }

    })

    private val onClipboardUpdate =
        ClipboardManager.OnClipboardUpdateListener { setClipboard(it.text) }

    fun start() {
        if (lifecycle.currentState != FcitxLifecycle.State.STOPPED) {
            Timber.w("Skip starting fcitx: not at stopped state!")
            return
        }
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_START)
        ClipboardManager.addOnUpdateListener(onClipboardUpdate)
        dispatcher.start()
    }

    fun stop() {
        if (lifecycle.currentState != FcitxLifecycle.State.READY) {
            Timber.w("Skip stopping fcitx: not at ready state!")
            return
        }
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_STOP)
        Timber.i("stop")
        ClipboardManager.removeOnUpdateListener(onClipboardUpdate)
        dispatcher.stop().let {
            if (it.isNotEmpty())
                Timber.w("${it.size} job(s) didn't get a chance to run!")
        }
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_STOPPED)
    }

}