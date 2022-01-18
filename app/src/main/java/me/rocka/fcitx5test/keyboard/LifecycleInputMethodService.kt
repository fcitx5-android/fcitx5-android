package me.rocka.fcitx5test.keyboard

import android.inputmethodservice.InputMethodService
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

open class LifecycleInputMethodService : InputMethodService(), LifecycleOwner {
    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}