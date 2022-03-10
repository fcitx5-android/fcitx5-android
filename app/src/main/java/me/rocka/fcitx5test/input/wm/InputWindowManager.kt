package me.rocka.fcitx5test.input.wm

import android.transition.Slide
import android.transition.TransitionManager
import android.view.ViewGroup
import android.widget.FrameLayout
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.broadcast.InputBroadcaster
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.keyboard.KeyboardWindow
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.minusAssign
import org.mechdancer.dependency.plusAssign
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout

class InputWindowManager : UniqueViewComponent<InputWindowManager, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val broadcaster: InputBroadcaster by manager.must()
    private val keyboardWindow: KeyboardWindow by manager.must()
    private lateinit var scope: DynamicScope

    var currentWindow: InputWindow<*>? = null
        private set

    private fun prepareAnimation() {
        val slide = Slide()
        TransitionManager.beginDelayedTransition(view, slide)
    }

    fun attachWindow(window: InputWindow<*>, animation: Boolean = true) {
        if (window.isAttached)
            throw IllegalArgumentException("$window is already attached")
        currentWindow = window
        scope += window
        val windowView = window.view
        if (animation) {
            prepareAnimation()
        }
        view.add(
            windowView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        broadcaster.onWindowAttached(window)
    }

    fun switchToKeyboardWindow(animation: Boolean = true) {
        if (currentWindow is KeyboardWindow)
            return
        currentWindow?.let {
            scope -= it
            currentWindow = null
            if (animation) {
                prepareAnimation()
            }
            view.removeAllViews()
            broadcaster.onWindowDetached(it)
            attachWindow(keyboardWindow, false)
        }
    }

    fun showWindow() {
        if (currentWindow == null)
            attachWindow(keyboardWindow, false)
        currentWindow?.onShow()
    }

    override val view: FrameLayout by lazy { context.frameLayout(R.id.input_window) }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        this.scope = scope
    }
}