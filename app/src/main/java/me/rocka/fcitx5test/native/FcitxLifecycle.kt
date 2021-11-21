package me.rocka.fcitx5test.native

enum class FcitxState {
    Starting,
    Ready,
    Stopping,
    Stopped
}

interface FcitxLifecycleObserver {
    fun onReady()
    fun onStopped()
}

interface FcitxLifecycleOwner {

    val currentState: FcitxState

    var observer: FcitxLifecycleObserver?

    fun start()

    fun stop()

}