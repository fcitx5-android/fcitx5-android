package org.fcitx.fcitx5.android.input.status

import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.utils.AppUtil
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.lParams
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource
import splitties.views.recyclerview.gridLayoutManager

class StatusAreaWindow : InputWindow.ExtendedInputWindow<StatusAreaWindow>(),
    InputBroadcastReceiver {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val fcitx: Fcitx by manager.fcitx()

    private val adapter: StatusAreaAdapter by lazy {
        object : StatusAreaAdapter() {
            override fun onItemClick(actionId: Int) {
                service.lifecycleScope.launch {
                    fcitx.activateAction(actionId)
                }
            }
        }
    }

    val view by lazy {
        context.recyclerView {
            layoutManager = gridLayoutManager(4)
            adapter = this@StatusAreaWindow.adapter
        }
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) {
        adapter.entries = actions
    }

    override fun onCreateView() = view.apply {
        settingsButton.setOnClickListener {
            AppUtil.launchMain(context)
        }
    }

    override val title: String = ""

    private val settingsButton by lazy {
        context.imageButton {
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            imageResource = R.drawable.ic_baseline_settings_24
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    private val barExtension by lazy {
        context.horizontalLayout {
            add(settingsButton, lParams(dp(40), dp(40)))
        }
    }

    override fun onCreateBarExtension() = barExtension

    override fun onAttached() {
        service.lifecycleScope.launch {
            adapter.entries = fcitx.statusArea()
        }
    }

    override fun onDetached() {
    }
}
