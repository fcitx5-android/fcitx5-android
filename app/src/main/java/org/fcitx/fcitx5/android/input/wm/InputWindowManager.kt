package org.fcitx.fcitx5.android.input.wm

import android.view.View
import android.widget.FrameLayout
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.utils.isUiThread
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.minusAssign
import org.mechdancer.dependency.plusAssign
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import timber.log.Timber

class InputWindowManager : UniqueViewComponent<InputWindowManager, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val broadcaster: InputBroadcaster by manager.must()
    private lateinit var scope: DynamicScope

    private val essentialWindows = mutableMapOf<EssentialWindow.Key, Pair<InputWindow, View>>()

    private var currentWindow: InputWindow? = null
    private var currentView: View? = null

    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private fun prepareAnimation(enterAnimation: Transition?, remove: View, add: View) {
        if (disableAnimation)
            return
        enterAnimation?.addTarget(add)
        val fade = Fade().apply {
            addTarget(remove)
        }
        TransitionManager.beginDelayedTransition(view, TransitionSet().apply {
            enterAnimation?.let { addTransition(it) }
            addTransition(fade)
            duration = 100
        })
    }

    /**
     * Attach an essential window by key
     * IMPORTANT: the view of this essential window must have been initialized,
     * i.e. another [attachWindow] that accepts the window instance should have been used at least once
     * before using this function
     */
    fun attachWindow(windowKey: EssentialWindow.Key) {
        ensureThread()
        essentialWindows[windowKey]?.let { (window, _) ->
            attachWindow(window)
        } ?: throw IllegalStateException("$windowKey is not a known essential window key")
    }

    /**
     * Remove an essential window
     */
    fun removeEssentialWindow(windowKey: EssentialWindow.Key) {
        val (window, _) = essentialWindows[windowKey]
            ?: throw IllegalStateException("$windowKey is not a known essential window key")
        if (currentWindow === window)
            throw IllegalStateException("$windowKey cannot be removed when it's active")
        // remove from scope
        scope -= window
        // remove from map
        essentialWindows.remove(windowKey)
    }

    /**
     * Attach a new window, removing the old one
     * This function initialize the view for windows and save it for essential windows
     */
    fun attachWindow(window: InputWindow) {
        ensureThread()
        if (window === currentWindow)
            Timber.d("Skip attaching $window")
        val newView = if (window is EssentialWindow) {
            // keep the view for essential windows
            essentialWindows[window.key]?.second ?: window.onCreateView()
                .also { essentialWindows[window.key] = window to it }
        } else {
            // add the new window to scope, except essential windows (they are always in scope)
            scope += window
            window.onCreateView()
        }
        if (currentWindow != null) {
            val oldWindow = currentWindow!!
            val oldView = currentView!!
            prepareAnimation(window.enterAnimation, oldView, newView)
            // notify the window that it will be detached
            oldWindow.onDetached()
            // remove the old window from layout
            view.removeView(oldView)
            // broadcast the old window was removed from layout
            broadcaster.onWindowDetached(oldWindow)
            Timber.d("Detach $oldWindow")
            // finally remove the old window from scope only if it's not an essential window,
            if (oldWindow !is EssentialWindow)
                scope -= oldWindow
        }
        // add the new window to layout
        view.apply { add(newView, lParams(matchParent, matchParent)) }
        currentView = newView
        Timber.d("Attach $window")
        // notify the window it was attached
        window.onAttached()
        currentWindow = window
        // broadcast the new window was added to layout
        broadcaster.onWindowAttached(window)
    }

    override val view: FrameLayout by lazy { context.frameLayout(R.id.input_window) }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        this.scope = scope
    }

    private fun ensureThread() {
        if (!isUiThread())
            throw IllegalThreadStateException("Window manager must be operated in main thread!")
    }
}