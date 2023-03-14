package org.fcitx.fcitx5.android.input

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner

open class LifecycleInputMethodService : InputMethodService(), LifecycleOwner {
    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    override val lifecycle = lifecycleRegistry

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @CallSuper
    override fun onCreateInputView(): View? {
        window.window!!.decorView.setViewTreeLifecycleOwner(this)
        return null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}