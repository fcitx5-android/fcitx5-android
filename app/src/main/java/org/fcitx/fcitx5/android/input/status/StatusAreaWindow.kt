package org.fcitx.fcitx5.android.input.status

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.editing.TextEditingUi.GImageButton
import org.fcitx.fcitx5.android.input.status.StatusAreaEntry.Android.Type.*
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.borderlessRippleDrawable
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.lParams
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.padding
import splitties.views.recyclerview.gridLayoutManager

class StatusAreaWindow : InputWindow.ExtendedInputWindow<StatusAreaWindow>(),
    InputBroadcastReceiver {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val fcitx: Fcitx by manager.fcitx()
    private val theme by manager.theme()

    private val staticEntries by lazy {
        arrayOf(
            StatusAreaEntry.Android(
                context.getString(R.string.theme),
                R.drawable.ic_baseline_palette_24,
                ThemeList
            ),
            StatusAreaEntry.Android(
                context.getString(R.string.input_method_options),
                R.drawable.ic_baseline_language_24,
                InputMethod
            ),
            StatusAreaEntry.Android(
                context.getString(R.string.reload_config),
                R.drawable.ic_baseline_sync_24,
                ReloadConfig
            ),
            StatusAreaEntry.Android(
                context.getString(R.string.behavior),
                R.drawable.ic_baseline_keyboard_24,
                Behavior
            )
        )
    }

    private val adapter: StatusAreaAdapter by lazy {
        object : StatusAreaAdapter() {
            override fun onItemClick(it: StatusAreaEntry) {
                service.lifecycleScope.launch {
                    when (it) {
                        is StatusAreaEntry.Fcitx -> fcitx.activateAction(it.action.id)
                        is StatusAreaEntry.Android -> when (it.type) {
                            GlobalOptions -> AppUtil.launchMainToConfig(
                                context, MainActivity.INTENT_DATA_CONFIG_GLOBAL
                            )
                            InputMethod -> fcitx.currentIme().let {
                                AppUtil.launchMainToConfig(
                                    context, MainActivity.INTENT_DATA_CONFIG_IM,
                                    bundleOf(
                                        InputMethodConfigFragment.ARG_NAME to it.displayName,
                                        InputMethodConfigFragment.ARG_UNIQUE_NAME to it.uniqueName
                                    )
                                )
                            }
                            ReloadConfig -> {
                                fcitx.reloadConfig()
                                Toast.makeText(service, R.string.done, Toast.LENGTH_SHORT).show()
                            }
                            Behavior -> AppUtil.launchMainToConfig(
                                context, MainActivity.INTENT_DATA_CONFIG_BEHAVIOR
                            )
                            ThemeList -> AppUtil.launchMainToConfig(
                                context, MainActivity.INTENT_DATA_CONFIG_THEME
                            )
                        }
                    }
                }
            }

            override val theme: Theme
                get() = this@StatusAreaWindow.theme
        }
    }

    val view by lazy {
        context.recyclerView {
            if (!ThemeManager.prefs.keyBorder.getValue()) {
                backgroundColor = theme.barColor.color
            }
            layoutManager = gridLayoutManager(4)
            adapter = this@StatusAreaWindow.adapter
        }
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) {
        adapter.entries = arrayOf(
            *staticEntries,
            *actions.map { StatusAreaEntry.fromAction(it) }.toTypedArray()
        )
    }

    override fun onCreateView() = view.apply {
        settingsButton.setOnClickListener {
            AppUtil.launchMain(context)
        }
    }

    override val title: String = ""

    private val settingsButton by lazy {
        GImageButton(context).apply {
            background = borderlessRippleDrawable(theme.keyPressHighlightColor.color, dp(20))
            colorFilter = PorterDuffColorFilter(theme.altKeyTextColor.color, PorterDuff.Mode.SRC_IN)
            imageResource = R.drawable.ic_baseline_settings_24
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            padding = dp(10)
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
            onStatusAreaUpdate(fcitx.statusArea())
        }
    }

    override fun onDetached() {
    }
}
