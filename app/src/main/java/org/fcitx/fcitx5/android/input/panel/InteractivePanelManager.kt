/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.panel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanel
import org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanelHost
import org.fcitx.fcitx5.android.core.FcitxPluginServices
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.core.data.PluginDescriptor
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.IUniqueComponent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.manager.DependencyManager
import timber.log.Timber
import kotlin.reflect.KClass

/**
 * Manages the interactive input panel plugin: binds to the plugin's
 * [IInteractiveInputPanel] service, forwards the panel surface and touch events,
 * and publishes candidates / commits text on behalf of the plugin.
 */
class InteractivePanelManager : IUniqueComponent<InteractivePanelManager>, Dependent {

    private val manager = DependencyManager()

    private val context by manager.context()
    private val service by manager.inputMethodService()

    private val mainHandler = Handler(Looper.getMainLooper())

    override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

    override fun equals(other: Any?): Boolean = defaultEquals(other)

    override fun hashCode(): Int = defaultHashCode()

    override fun toString(): String = javaClass.name

    /**
     * Listener for candidates published by the plugin.
     * Must be called on the main thread.
     */
    fun interface OnCandidatesListener {
        fun onCandidatesChanged(candidates: List<String>)
    }

    var candidatesListener: OnCandidatesListener? = null

    /**
     * Called when the plugin's binding died unexpectedly (e.g. the plugin
     * was uninstalled or crashed). Must be called on the main thread.
     */
    var onPanelDied: (() -> Unit)? = null

    /**
     * Host interface implemented by fcitx5-android, exposed to the plugin.
     * All binder calls are dispatched to the main thread.
     */
    private val host = object : IInteractiveInputPanelHost.Stub() {
        override fun setCandidates(candidates: MutableList<String>?) {
            val list = candidates?.toList() ?: emptyList()
            Timber.d("Interactive panel candidates: $list")
            mainHandler.post {
                candidatesListener?.onCandidatesChanged(list)
            }
        }

        override fun commitText(text: String?) {
            if (text.isNullOrEmpty()) return
            mainHandler.post {
                service.commitText(text)
            }
        }

        override fun requestHideSelf() {
            mainHandler.post {
                service.requestHideSelf(0)
            }
        }
    }

    /** Loaded plugins that declare an interactive input panel */
    private val panelPlugins: List<PluginDescriptor>
        get() = DataManager.getLoadedPlugins().filter { it.hasInteractivePanel }

    /** Whether any loaded plugin provides an interactive input panel */
    val hasPanelPlugin: Boolean
        get() = panelPlugins.isNotEmpty()

    private var panel: IInteractiveInputPanel? = null
    private var connection: ServiceConnection? = null

    /** Whether the panel window is currently attached (visible) */
    private var panelWindowAttached = false

    /** Cached surface state, replayed to the plugin once the binding is established */
    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    private var sessionId = 0

    private fun notifyAttach() {
        val p = panel ?: return
        try {
            Timber.d("Notify interactive panel attach, session=$sessionId")
            p.onAttach(sessionId, host)
        } catch (e: Throwable) {
            Timber.w("Failed to attach interactive panel session")
            Timber.w(e)
        }
    }

    private fun notifySurfaceCreated() {
        val p = panel ?: return
        val s = surface
        if (s == null || !s.isValid) return
        try {
            Timber.d("Notify interactive panel surface created: ${surfaceWidth}x$surfaceHeight")
            p.onSurfaceCreated(s, surfaceWidth, surfaceHeight)
        } catch (e: Throwable) {
            Timber.w("Failed to send surface to interactive panel")
            Timber.w(e)
        }
    }

    /**
     * A new input session starts. Must be called on the main thread.
     */
    fun onStartInput() {
        sessionId++
        Timber.d("Interactive panel session started: $sessionId")
        if (panelWindowAttached) {
            notifyAttach()
        }
    }

    /**
     * The current input session ends. Must be called on the main thread.
     */
    fun onFinishInput() {
        Timber.d("Interactive panel session ended: $sessionId")
        val p = panel ?: return
        try {
            p.onDetach()
        } catch (e: Throwable) {
            Timber.w("Failed to detach interactive panel session")
            Timber.w(e)
        }
    }

    /**
     * The panel window is attached. Must be called on the main thread.
     */
    fun attachPanelWindow() {
        Timber.d("Interactive panel window attached")
        panelWindowAttached = true
        if (panel != null) {
            notifyAttach()
            notifySurfaceCreated()
        } else {
            connect()
        }
    }

    /**
     * The panel window is detached. Must be called on the main thread.
     */
    fun detachPanelWindow() {
        Timber.d("Interactive panel window detached")
        panelWindowAttached = false
        // notify the plugin that the surface is going away before unbinding
        onSurfaceDestroyed()
        disconnect()
    }

    /**
     * The panel surface was created. Must be called on the main thread.
     */
    fun onSurfaceCreated(surface: Surface, width: Int, height: Int) {
        Timber.d("Interactive panel surface created: ${width}x$height")
        this.surface = surface
        surfaceWidth = width
        surfaceHeight = height
        notifySurfaceCreated()
    }

    /**
     * The panel surface was destroyed. Must be called on the main thread.
     */
    fun onSurfaceDestroyed() {
        Timber.d("Interactive panel surface destroyed")
        surface = null
        surfaceWidth = 0
        surfaceHeight = 0
        val p = panel ?: return
        try {
            p.onSurfaceDestroyed()
        } catch (e: Throwable) {
            Timber.w("Failed to notify interactive panel surface destroyed")
            Timber.w(e)
        }
    }

    /**
     * The panel size changed. Must be called on the main thread.
     */
    fun onSizeChanged(width: Int, height: Int) {
        Timber.d("Interactive panel size changed: ${width}x$height")
        val p = panel ?: return
        try {
            p.onSizeChanged(width, height)
        } catch (e: Throwable) {
            Timber.w("Failed to notify interactive panel size change")
            Timber.w(e)
        }
    }

    /**
     * A new stroke begins. Must be called on the main thread.
     */
    fun onTouchDown(x: Float, y: Float) {
        panel?.let { p ->
            try {
                p.onTouchDown(x, y)
            } catch (e: Throwable) {
                Timber.w("Failed to send touch down to interactive panel")
                Timber.w(e)
            }
        }
    }

    /**
     * Batched stroke points. Must be called on the main thread.
     */
    fun onTouchMove(xs: FloatArray, ys: FloatArray) {
        panel?.let { p ->
            try {
                p.onTouchMove(xs, ys)
            } catch (e: Throwable) {
                Timber.w("Failed to send touch move to interactive panel")
                Timber.w(e)
            }
        }
    }

    /**
     * The current stroke ends. Must be called on the main thread.
     */
    fun onTouchUp(x: Float, y: Float) {
        panel?.let { p ->
            try {
                p.onTouchUp(x, y)
            } catch (e: Throwable) {
                Timber.w("Failed to send touch up to interactive panel")
                Timber.w(e)
            }
        }
    }

    /**
     * Commit text through the current input session.
     * Must be called on the main thread.
     */
    fun commitText(text: String) {
        service.commitText(text)
    }

    private fun connect() {
        val plugin = panelPlugins.firstOrNull()
        if (plugin == null) {
            Timber.w("No interactive panel plugin found")
            onPanelDied?.invoke()
            return
        }
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                Timber.d("Interactive panel connected: $name")
                panel = IInteractiveInputPanel.Stub.asInterface(binder)
                if (panelWindowAttached) {
                    notifyAttach()
                    notifySurfaceCreated()
                }
            }

            // may re-connect in the future
            override fun onServiceDisconnected(name: ComponentName) {
                Timber.d("Interactive panel disconnected: $name")
                panel = null
            }

            // will never receive another connection
            override fun onBindingDied(name: ComponentName?) {
                Timber.d("Interactive panel binding died: $name")
                panel = null
                connection = null
                mainHandler.post {
                    onPanelDied?.invoke()
                }
            }
        }
        connection = conn
        try {
            Timber.d("Bind to interactive panel: ${plugin.name}")
            val ok = context.bindService(
                Intent(FcitxPluginServices.PLUGIN_PANEL_ACTION).apply {
                    setPackage(plugin.packageName)
                },
                conn,
                Context.BIND_AUTO_CREATE
            )
            if (!ok) throw Exception("Couldn't find interactive panel service or not enough permission")
        } catch (e: Exception) {
            runCatching { context.unbindService(conn) }
            connection = null
            Timber.w("Cannot bind to interactive panel: ${plugin.name}")
            Timber.w(e)
            mainHandler.post {
                onPanelDied?.invoke()
            }
        }
    }

    private fun disconnect() {
        connection?.let {
            runCatching { context.unbindService(it) }
        }
        connection = null
        panel = null
    }

    final override fun handle(scopeEvent: ScopeEvent) = manager.handle(scopeEvent)
}
