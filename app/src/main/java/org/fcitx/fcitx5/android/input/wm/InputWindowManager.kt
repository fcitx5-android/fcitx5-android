package org.fcitx.fcitx5.android.input.wm

import android.view.View
import android.widget.FrameLayout
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
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
    private val keyboardWindow: KeyboardWindow by manager.must()
    private lateinit var scope: DynamicScope

    var currentWindow: InputWindow? = null
        private set

    private fun prepareAnimation(remove: View, add: View) {
        val slide = Slide().apply {
            addTarget(add)
        }
        val fade = Fade().apply {
            addTarget(remove)
        }
        TransitionManager.beginDelayedTransition(view, TransitionSet().apply {
            addTransition(slide)
            addTransition(fade)
            duration = 100
        })
    }

    /**
     * Attach a new window, removing the old one
     */
    fun attachWindow(window: InputWindow) {
        if (window == currentWindow)
            throw IllegalArgumentException("$window is already attached")
        // add the new window to scope
        scope += window
        currentWindow?.let {
            prepareAnimation(it.view, window.view)
            // notify the window that it will be detached
            it.onDetached()
            // remove the old window from layout
            view.removeView(it.view)
            // broadcast the old window was removed from layout
            broadcaster.onWindowDetached(it)
            Timber.d("Detach $it")
            // finally remove the old window from scope only if it's not keyboard window,
            // because keyboard window is always in scope
            if (it !is KeyboardWindow)
                scope -= it
        }
        // add the new window to layout
        view.apply { add(window.view, lParams(matchParent, matchParent)) }
        Timber.d("Attach $window")
        // notify the window it was attached
        window.onAttached()
        currentWindow = window
        // broadcast the new window was added to layout
        broadcaster.onWindowAttached(window)
    }

    /**
     * Remove current window and attach keyboard window
     */
    fun switchToKeyboardWindow() {
        if (currentWindow === keyboardWindow) {
            return
        }
        attachWindow(keyboardWindow)
    }

    override val view: FrameLayout by lazy { context.frameLayout(R.id.input_window) }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        this.scope = scope
    }
}