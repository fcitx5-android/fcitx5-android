package org.fcitx.fcitx5.android.input.preedit

import android.view.View
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler

class PreeditComponent : UniqueComponent<PreeditComponent>(), Dependent, InputBroadcastReceiver,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val theme by manager.theme()

    private val content = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", 0, "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )

    val ui by lazy { PreeditUi(context, theme) }

    override fun onInputPanelAuxUpdate(data: FcitxEvent.InputPanelAuxEvent.Data) {
        content.aux = data
        updateView()
    }

    override fun onPreeditUpdate(data: FcitxEvent.PreeditEvent.Data) {
        content.preedit = data
        updateView()
    }

    private fun updateView() {
        ui.update(content)
        ui.root.visibility = if (ui.visible) View.VISIBLE else View.INVISIBLE
    }
}
