package org.fcitx.fcitx5.android.input.preedit

import android.view.Gravity
import android.view.WindowManager
import android.widget.PopupWindow
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler

class PreeditComponent : UniqueComponent<PreeditComponent>(), Dependent, InputBroadcastReceiver,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val inputView by manager.inputView()
    private val theme by manager.theme()

    private val cachedPreedit = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", 0, "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )
    private val preeditUi by lazy { PreeditUi(context, theme) }
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

    override fun onInputPanelAuxUpdate(data: FcitxEvent.InputPanelAuxEvent.Data) {
        cachedPreedit.aux = data
        updatePreedit()
    }

    override fun onPreeditUpdate(data: FcitxEvent.PreeditEvent.Data) {
        cachedPreedit.preedit = data
        updatePreedit()
    }

    private fun updatePreedit() {
        preeditUi.update(cachedPreedit)
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