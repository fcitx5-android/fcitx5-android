package me.rocka.fcitx5test.input.bar

import android.widget.LinearLayout
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.input.wm.InputWindowManager
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, LinearLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()


    // TODO: use windowManager here to switch input windows
    private val windowManager: InputWindowManager by manager.must()

    override fun onScopeSetupFinished(scope: DynamicScope) {
        // TODO: initialize candidate component here
    }

    override val view: LinearLayout by lazy {
        TODO("Not yet implemented")
    }

    override fun onWindowAttached(window: InputWindow<*>) {
        // TODO: display window.title and window.barExtension
    }

    override fun onWindowDetached(window: InputWindow<*>) {
        // TODO: cleanup
    }
}