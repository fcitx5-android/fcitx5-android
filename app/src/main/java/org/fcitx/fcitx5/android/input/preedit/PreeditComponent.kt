package org.fcitx.fcitx5.android.input.preedit

import android.view.Gravity
import android.view.WindowManager
import android.widget.PopupWindow
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class PreeditComponent : UniqueComponent<PreeditComponent>(), Dependent,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val broadcaster: InputBroadcaster by manager.must()
    private val inputView by manager.inputView()

    private val cachedPreedit = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )
    private val preeditUi by lazy { PreeditUi(context) }
    private val preeditPopup by lazy {
        PopupWindow(
            preeditUi.root,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            isTouchable = false
            isClippingEnabled = false
            width = context.resources.displayMetrics.widthPixels
        }
    }

    fun dismiss() {
        preeditPopup.dismiss()
    }

    fun updateAux(aux: FcitxEvent.InputPanelAuxEvent) {
        cachedPreedit.aux = aux.data
        updatePreedit()
    }

    fun updatePreedit(preedit: FcitxEvent.PreeditEvent) {
        cachedPreedit.preedit = preedit.data
        updatePreedit()
    }

    private fun updatePreedit() {
        preeditUi.update(cachedPreedit)
        broadcaster.onPreeditUpdate(cachedPreedit)
        preeditPopup.run {
            if (!preeditUi.visible) {
                dismiss()
                return
            }
            val height = preeditUi.measureHeight(width)
            if (isShowing) {
                update(
                    0, -height,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            } else {
                showAtLocation(inputView, Gravity.NO_GRAVITY, 0, -height)
            }
        }
    }

}