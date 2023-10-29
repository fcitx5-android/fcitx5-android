/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.content.Context
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.utils.ImmutableGraph
import org.fcitx.fcitx5.android.utils.Locales
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber

/**
 * Do not use this class directly, accessing fcitx via daemon instead
 */
class Fcitx(private val context: Context) : FcitxAPI, FcitxLifecycleOwner {

    private val lifecycleRegistry = FcitxLifecycleRegistry()

    override val eventFlow = eventFlow_.asSharedFlow()

    override val isReady
        get() = lifecycle.currentState == FcitxLifecycle.State.READY

    override var inputMethodEntryCached =
        InputMethodEntry(context.getString(R.string._not_available_))
        private set

    override var statusAreaActionsCached: Array<Action> = emptyArray()
        private set

    override var clientPreeditCached = FormattedText.Empty
        private set

    override var inputPanelCached = FcitxEvent.InputPanelEvent.Data()
        private set

    // the computation is delayed to the first call of [getAddonReverseDependencies]
    private var addonGraph: ImmutableGraph<String, FcitxAPI.AddonDep>? = null

    private val addonReverseDependencies =
        mutableMapOf<String, List<Pair<String, FcitxAPI.AddonDep>>>()

    override fun getAddonReverseDependencies(addon: String) =
        (addonGraph ?: run { computeAddonGraph().also { addonGraph = it } }).let { graph ->
            addonReverseDependencies.computeIfAbsent(addon)
            {
                graph.bfs(it) { level, _, dep ->
                    // stop when the direct child is an optional dependency
                    dep == FcitxAPI.AddonDep.Required
                            || (level == 1 && dep == FcitxAPI.AddonDep.Optional)
                }
            }
        }

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
        withFcitxContext { availableInputMethods() ?: emptyArray() }

    override suspend fun enabledIme() =
        withFcitxContext { listInputMethods() ?: emptyArray() }

    override suspend fun setEnabledIme(array: Array<String>) =
        withFcitxContext { setEnabledInputMethods(array) }

    override suspend fun toggleIme() = withFcitxContext { toggleInputMethod() }
    override suspend fun activateIme(ime: String) = withFcitxContext { setInputMethod(ime) }
    override suspend fun enumerateIme(forward: Boolean) =
        withFcitxContext { nextInputMethod(forward) }

    override suspend fun currentIme() =
        withFcitxContext { inputMethodStatus() ?: inputMethodEntryCached }

    override suspend fun getGlobalConfig() = withFcitxContext {
        getFcitxGlobalConfig() ?: RawConfig()
    }

    override suspend fun setGlobalConfig(config: RawConfig) = withFcitxContext {
        setFcitxGlobalConfig(config)
    }

    override suspend fun getAddonConfig(addon: String) = withFcitxContext {
        getFcitxAddonConfig(addon) ?: RawConfig()
    }

    override suspend fun setAddonConfig(addon: String, config: RawConfig) = withFcitxContext {
        setFcitxAddonConfig(addon, config)
    }

    override suspend fun getAddonSubConfig(addon: String, path: String) = withFcitxContext {
        getFcitxAddonSubConfig(addon, path) ?: RawConfig()
    }

    override suspend fun setAddonSubConfig(addon: String, path: String, config: RawConfig) =
        withFcitxContext { setFcitxAddonSubConfig(addon, path, config) }

    override suspend fun getImConfig(key: String) = withFcitxContext {
        getFcitxInputMethodConfig(key) ?: RawConfig()
    }

    override suspend fun setImConfig(key: String, config: RawConfig) = withFcitxContext {
        setFcitxInputMethodConfig(key, config)
    }

    override suspend fun addons() = withFcitxContext { getFcitxAddons() ?: emptyArray() }
    override suspend fun setAddonState(name: Array<String>, state: BooleanArray) =
        withFcitxContext { setFcitxAddonState(name, state) }

    override suspend fun triggerQuickPhrase() = withFcitxContext { triggerQuickPhraseInput() }
    override suspend fun triggerUnicode() = withFcitxContext { triggerUnicodeInput() }
    private suspend fun setClipboard(string: String) =
        withFcitxContext { setFcitxClipboard(string) }

    override suspend fun focus(focus: Boolean) = withFcitxContext { focusInputContext(focus) }
    override suspend fun activate(uid: Int, pkgName: String) =
        withFcitxContext { activateInputContext(uid, pkgName) }

    override suspend fun deactivate(uid: Int) = withFcitxContext { deactivateInputContext(uid) }
    override suspend fun setCapFlags(flags: CapabilityFlags) =
        withFcitxContext { setCapabilityFlags(flags.toLong()) }

    override suspend fun statusArea(): Array<Action> =
        withFcitxContext { getFcitxStatusAreaActions() ?: emptyArray() }

    override suspend fun activateAction(id: Int) =
        withFcitxContext { activateUserInterfaceAction(id) }

    override suspend fun getCandidates(offset: Int, limit: Int): Array<String> =
        withFcitxContext { getFcitxCandidates(offset, limit) ?: emptyArray() }

    init {
        if (lifecycle.currentState != FcitxLifecycle.State.STOPPED)
            throw IllegalAccessException("Fcitx5 has already been created!")
    }


    override val lifecycle: FcitxLifecycle
        get() = lifecycleRegistry

    private companion object JNI {

        /**
         * called from native-lib
         */
        @Suppress("unused")
        @JvmStatic
        fun showToast(s: String) {
            ContextCompat.getMainExecutor(appContext).execute {
                appContext.toast(s)
            }
        }

        private val eventFlow_ =
            MutableSharedFlow<FcitxEvent<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

        private val fcitxEventHandlers = ArrayList<(FcitxEvent<*>) -> Unit>()

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
            extData: String,
            extCache: String,
            extDomains: Array<String>,
            libraryNames: Array<String>,
            libraryDependencies: Array<Array<String>>
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
        external fun toggleInputMethod()

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
        external fun activateInputContext(uid: Int, pkgName: String)

        @JvmStatic
        external fun deactivateInputContext(uid: Int)

        @JvmStatic
        external fun setCapabilityFlags(flags: Long)

        @JvmStatic
        external fun getFcitxStatusAreaActions(): Array<Action>?

        @JvmStatic
        external fun activateUserInterfaceAction(id: Int)

        @JvmStatic
        external fun getFcitxCandidates(offset: Int, limit: Int): Array<String>?

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
            }
            fcitxEventHandlers.forEach { it.invoke(event) }
            eventFlow_.tryEmit(event)
        }

        // will be called in fcitx main thread
        private fun onFirstRun() {
            Timber.i("onFirstRun")
            getFcitxGlobalConfig()?.get("cfg")?.apply {
                get("Behavior").apply {
                    get("ShareInputState").value = "All"
                    get("PreeditEnabledByDefault").value = "False"
                }
                setFcitxGlobalConfig(this)
            }
            getFcitxAddonConfig("pinyin")?.get("cfg")?.apply {
                get("PreeditInApplication").value = "False"
                get("PreeditCursorPositionAtBeginning").value = "False"
                get("QuickPhraseKey").value = ""
                setFcitxAddonConfig("pinyin", this)
            }
            firstRun = false
        }

        /**
         * register a [FcitxEvent] handler that will fire before events go into [eventFlow_]
         */
        private fun registerFcitxEventHandler(handler: (FcitxEvent<*>) -> Unit) {
            if (fcitxEventHandlers.contains(handler)) return
            fcitxEventHandlers.add(handler)
        }

        private fun unregisterFcitxEventHandler(handler: (FcitxEvent<*>) -> Unit) {
            fcitxEventHandlers.remove(handler)
        }

    }

    private val dispatcher = FcitxDispatcher(object : FcitxDispatcher.FcitxController {
        override fun nativeStartup() {
            DataManager.sync()
            val locale = Locales.fcitxLocale
            val dataDir = DataManager.dataDir.absolutePath
            val plugins = DataManager.getLoadedPlugins()
            val nativeLibDir = StringBuilder(context.applicationInfo.nativeLibraryDir)
            val extDomains = arrayListOf<String>()
            val libraryNames = arrayListOf<String>()
            val libraryDependency = arrayListOf<Array<String>>()
            plugins.forEach {
                nativeLibDir.append(':')
                nativeLibDir.append(it.nativeLibraryDir)
                it.domain?.let { d ->
                    extDomains.add(d)
                }
                it.libraryDependency.forEach { (lib, dep) ->
                    libraryNames.add(lib)
                    libraryDependency.add(dep.toTypedArray())
                }
            }
            Timber.d(
                """
               Starting fcitx with:
               locale=$locale
               dataDir=$dataDir
               nativeLibDir=$nativeLibDir
               extDomains=${extDomains.joinToString()}
            """.trimIndent()
            )
            with(FcitxApplication.getInstance().directBootAwareContext) {
                startupFcitx(
                    locale,
                    dataDir,
                    nativeLibDir.toString(),
                    (getExternalFilesDir(null) ?: filesDir).absolutePath,
                    (externalCacheDir ?: cacheDir).absolutePath,
                    extDomains.toTypedArray(),
                    libraryNames.toTypedArray(),
                    libraryDependency.toTypedArray()
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

    private suspend inline fun <T> withFcitxContext(crossinline block: suspend () -> T): T =
        withContext(dispatcher) {
            block()
        }

    @Keep
    private val onClipboardUpdate = ClipboardManager.OnClipboardUpdateListener {
        lifecycle.lifecycleScope.launch { setClipboard(it.text) }
    }

    private fun computeAddonGraph() = runBlocking {
        addons().flatMap { a ->
            a.dependencies.map {
                ImmutableGraph.Edge(it, a.uniqueName, FcitxAPI.AddonDep.Required)
            } + a.optionalDependencies.map {
                ImmutableGraph.Edge(it, a.uniqueName, FcitxAPI.AddonDep.Optional)
            }
        }.let { ImmutableGraph(it) }
    }

    private fun handleFcitxEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.ReadyEvent -> lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_READY)
            is FcitxEvent.IMChangeEvent -> inputMethodEntryCached = event.data
            is FcitxEvent.StatusAreaEvent -> {
                val (actions, im) = event.data
                statusAreaActionsCached = actions
                // Engine subMode update won't trigger IMChangeEvent, but usually updates StatusArea
                if (im != inputMethodEntryCached) {
                    inputMethodEntryCached = im
                    // notify downstream consumers that engine subMode has changed
                    eventFlow_.tryEmit(FcitxEvent.IMChangeEvent(im))
                }
            }
            is FcitxEvent.ClientPreeditEvent -> clientPreeditCached = event.data
            is FcitxEvent.InputPanelEvent -> inputPanelCached = event.data
            else -> {}
        }
    }

    fun start() {
        if (lifecycle.currentState != FcitxLifecycle.State.STOPPED) {
            Timber.w("Skip starting fcitx: not at stopped state!")
            return
        }
        registerFcitxEventHandler(::handleFcitxEvent)
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_START)
        ClipboardManager.addOnUpdateListener(onClipboardUpdate)
        DataManager.addOnNextSyncedCallback {
            FcitxPluginServices.connectAll()
        }
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
        FcitxPluginServices.disconnectAll()
        dispatcher.stop().let {
            if (it.isNotEmpty())
                Timber.w("${it.size} job(s) didn't get a chance to run!")
        }
        lifecycleRegistry.postEvent(FcitxLifecycle.Event.ON_STOPPED)
        unregisterFcitxEventHandler(::handleFcitxEvent)
        // clear addon graph
        addonGraph = null
        addonReverseDependencies.clear()
    }

}