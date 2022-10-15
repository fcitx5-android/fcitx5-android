package org.fcitx.fcitx5.android.core

import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.DataManager
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.utils.ImmutableGraph
import timber.log.Timber

class Fcitx(private val context: Context) : FcitxAPI, FcitxLifecycleOwner by JNI {

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    override val eventFlow = eventFlow_.asSharedFlow().apply {
        onEach {
            when (it) {
                is FcitxEvent.IMChangeEvent -> inputMethodEntryCached = it.data
                is FcitxEvent.StatusAreaEvent -> statusAreaActionsCached = it.data
                else -> {}
            }
        }.launchIn(lifecycle.lifecycleScope)
    }

    override val isReady
        get() = lifecycle.currentState == FcitxLifecycle.State.READY

    override var inputMethodEntryCached =
        InputMethodEntry(context.getString(R.string._not_available_))
        private set

    override var statusAreaActionsCached: Array<Action> = arrayOf()
        private set

    enum class AddonDep {
        Required,
        Optional
    }

    private val addonGraph: ImmutableGraph<String, AddonDep> by lazy {
        runBlocking {
            addons().flatMap { a ->
                a.dependencies.map {
                    ImmutableGraph.Edge(it, a.uniqueName, AddonDep.Required)
                } + a.optionalDependencies.map {
                    ImmutableGraph.Edge(it, a.uniqueName, AddonDep.Optional)
                }
            }.let { ImmutableGraph(it) }
        }
    }

    private val addonReversedDependencies = mutableMapOf<String, List<Pair<String, AddonDep>>>()

    override fun getAddonReverseDependencies(addon: String) =
        addonReversedDependencies.computeIfAbsent(addon) { addonGraph.bfs(it) }

    override fun translate(str: String, domain: String) = getFcitxTranslation(domain, str)

    override suspend fun save() = withFcitxContext { saveFcitxState() }
    override suspend fun reloadConfig() = withFcitxContext { reloadFcitxConfig() }

    override suspend fun sendKey(key: String, states: UInt, up: Boolean, timestamp: Int) =
        withFcitxContext { sendKeyToFcitxString(key, states.toInt(), up, timestamp) }

    override suspend fun sendKey(c: Char, states: UInt, up: Boolean, timestamp: Int) =
        withFcitxContext { sendKeyToFcitxChar(c, states.toInt(), up, timestamp) }

    override suspend fun sendKey(sym: Int, states: UInt, up: Boolean, timestamp: Int) =
        withFcitxContext { sendKeySymToFcitx(sym, states.toInt(), up, timestamp) }

    override suspend fun sendKey(sym: KeySym, states: KeyStates, up: Boolean, timestamp: Int) =
        withFcitxContext { sendKeySymToFcitx(sym.sym, states.toInt(), up, timestamp) }

    override suspend fun select(idx: Int): Boolean = withFcitxContext { selectCandidate(idx) }
    override suspend fun isEmpty(): Boolean = withFcitxContext { isInputPanelEmpty() }
    override suspend fun reset() = withFcitxContext { resetInputContext() }
    override suspend fun moveCursor(position: Int) = withFcitxContext { repositionCursor(position) }
    override suspend fun availableIme() =
        withFcitxContext { availableInputMethods() ?: arrayOf() }

    override suspend fun enabledIme() =
        withFcitxContext { listInputMethods() ?: arrayOf() }

    override suspend fun setEnabledIme(array: Array<String>) =
        withFcitxContext { setEnabledInputMethods(array) }

    override suspend fun activateIme(ime: String) = withFcitxContext { setInputMethod(ime) }
    override suspend fun enumerateIme(forward: Boolean) =
        withFcitxContext { nextInputMethod(forward) }

    override suspend fun currentIme() =
        withFcitxContext { inputMethodStatus() ?: inputMethodEntryCached }

    override suspend fun getGlobalConfig() = withFcitxContext {
        getFcitxGlobalConfig() ?: RawConfig(arrayOf())
    }

    override suspend fun setGlobalConfig(config: RawConfig) = withFcitxContext {
        setFcitxGlobalConfig(config)
    }

    override suspend fun getAddonConfig(addon: String) = withFcitxContext {
        getFcitxAddonConfig(addon) ?: RawConfig(arrayOf())
    }

    override suspend fun setAddonConfig(addon: String, config: RawConfig) = withFcitxContext {
        setFcitxAddonConfig(addon, config)
    }

    override suspend fun getAddonSubConfig(addon: String, path: String) = withFcitxContext {
        getFcitxAddonSubConfig(addon, path) ?: RawConfig(arrayOf())
    }

    override suspend fun setAddonSubConfig(addon: String, path: String, config: RawConfig) =
        withFcitxContext { setFcitxAddonSubConfig(addon, path, config) }

    override suspend fun getImConfig(key: String) = withFcitxContext {
        getFcitxInputMethodConfig(key) ?: RawConfig(arrayOf())
    }

    override suspend fun setImConfig(key: String, config: RawConfig) = withFcitxContext {
        setFcitxInputMethodConfig(key, config)
    }

    override suspend fun addons() = withFcitxContext { getFcitxAddons() ?: arrayOf() }
    override suspend fun setAddonState(name: Array<String>, state: BooleanArray) =
        withFcitxContext { setFcitxAddonState(name, state) }

    override suspend fun triggerQuickPhrase() = withFcitxContext { triggerQuickPhraseInput() }
    override suspend fun triggerUnicode() = withFcitxContext { triggerUnicodeInput() }
    private suspend fun setClipboard(string: String) =
        withFcitxContext { setFcitxClipboard(string) }

    override suspend fun focus(focus: Boolean) = withFcitxContext { focusInputContext(focus) }
    override suspend fun activate(uid: Int) = withFcitxContext { activateInputContext(uid) }
    override suspend fun deactivate(uid: Int) = withFcitxContext { deactivateInputContext(uid) }
    override suspend fun setCapFlags(flags: CapabilityFlags) =
        withFcitxContext { setCapabilityFlags(flags.toLong()) }

    override suspend fun statusArea(): Array<Action> =
        withFcitxContext { getFcitxStatusAreaActions() ?: arrayOf() }

    override suspend fun activateAction(id: Int) =
        withFcitxContext { activateUserInterfaceAction(id) }

    init {
        if (lifecycle.currentState != FcitxLifecycle.State.STOPPED)
            throw IllegalAccessException("Fcitx5 has already been created!")
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
            setupLogStream(AppPrefs.getInstance().internal.verboseLog.getValue())
        }

        @JvmStatic
        external fun setupLogStream(verbose: Boolean)

        @JvmStatic
        external fun startupFcitx(
            locale: String,
            appData: String,
            appLib: String,
            extData: String
        )

        @JvmStatic
        external fun getFcitxTranslation(domain: String, str: String): String

        @JvmStatic
        external fun exitFcitx()

        @JvmStatic
        external fun saveFcitxState()

        @JvmStatic
        external fun reloadFcitxConfig()

        @JvmStatic
        external fun sendKeyToFcitxString(key: String, state: Int, up: Boolean, timestamp: Int)

        @JvmStatic
        external fun sendKeyToFcitxChar(c: Char, state: Int, up: Boolean, timestamp: Int)

        @JvmStatic
        external fun sendKeySymToFcitx(sym: Int, state: Int, up: Boolean, timestamp: Int)

        @JvmStatic
        external fun selectCandidate(idx: Int): Boolean

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
        external fun getFcitxAddonSubConfig(addon: String, path: String): RawConfig?

        @JvmStatic
        external fun getFcitxInputMethodConfig(im: String): RawConfig?

        @JvmStatic
        external fun setFcitxGlobalConfig(config: RawConfig)

        @JvmStatic
        external fun setFcitxAddonConfig(addon: String, config: RawConfig)

        @JvmStatic
        external fun setFcitxAddonSubConfig(addon: String, path: String, config: RawConfig)

        @JvmStatic
        external fun setFcitxInputMethodConfig(im: String, config: RawConfig)

        @JvmStatic
        external fun getFcitxAddons(): Array<AddonInfo>?

        @JvmStatic
        external fun setFcitxAddonState(name: Array<String>, state: BooleanArray)

        @JvmStatic
        external fun triggerQuickPhraseInput()

        @JvmStatic
        external fun triggerUnicodeInput()

        @JvmStatic
        external fun setFcitxClipboard(string: String)

        @JvmStatic
        external fun focusInputContext(focus: Boolean)

        @JvmStatic
        external fun activateInputContext(uid: Int)

        @JvmStatic
        external fun deactivateInputContext(uid: Int)

        @JvmStatic
        external fun setCapabilityFlags(flags: Long)

        @JvmStatic
        external fun getFcitxStatusAreaActions(): Array<Action>?

        @JvmStatic
        external fun activateUserInterfaceAction(id: Int)

        @JvmStatic
        external fun loopOnce()

        @JvmStatic
        external fun scheduleEmpty()

        private var firstRun by AppPrefs.getInstance().internal.firstRun

        /**
         * Called from native-lib
         */
        @Suppress("unused")
        @JvmStatic
        fun handleFcitxEvent(type: Int, params: Array<Any>) {
            val event = FcitxEvent.create(type, params)
            Timber.d("Handling $event")
            if (event is FcitxEvent.ReadyEvent) {
                if (firstRun) {
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
                get("QuickPhraseKey").value = ""
                setFcitxAddonConfig("pinyin", this)
            }
            firstRun = false
        }

        // will be called in fcitx main thread
        private fun onReady() {
            lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_READY)
        }

        override val lifecycle: FcitxLifecycle
            get() = lifecycleRegistry
    }

    private val dispatcher = FcitxDispatcher(object : FcitxDispatcher.FcitxController {
        override fun nativeStartup() {
            with(context) {
                DataManager.sync()
                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resources.configuration.locales.let {
                        buildString {
                            for (i in 0 until it.size()) {
                                if (i != 0) append(":")
                                append(it[i].run { "${language}_${country}:$language" })
                                // since there is not an `en.mo` file, `en` must be the only locale
                                // in order to use default english translation
                                if (i == 0 && it[i].language == "en") break
                            }
                        }
                    }
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

    private suspend fun <T> withFcitxContext(block: suspend () -> T): T =
        withContext(dispatcher) {
            block()
        }

    private val onClipboardUpdate = ClipboardManager.OnClipboardUpdateListener {
        lifecycle.lifecycleScope.launch { setClipboard(it) }
    }

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
        Timber.i("Fcitx stop()")
        ClipboardManager.removeOnUpdateListener(onClipboardUpdate)
        dispatcher.stop().let {
            if (it.isNotEmpty())
                Timber.w("${it.size} job(s) didn't get a chance to run!")
        }
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_STOPPED)
    }

}