package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.utils.AppUtil
import splitties.resources.dimenPxSize
import splitties.resources.styledDrawable
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.recyclerview.verticalLayoutManager
import splitties.views.topPadding

object InputMethodSwitcherDialog {
    suspend fun build(
        fcitx: FcitxAPI,
        service: FcitxInputMethodService,
        context: Context
    ): AlertDialog {
        val entries = InputMethodData.resolve(fcitx, service)
        val enabledIM = fcitx.inputMethodEntryCached.uniqueName
        val enabledIndex = entries.indexOfFirst { it.uniqueName == enabledIM }
        val dividerIndex = entries.indexOfFirst { it.ime }
        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.choose_input_method)
            .setView(context.recyclerView {
                layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                // add some padding because AlertDialog's `titleDividerNoCustom` won't show up...
                // but why?
                topPadding =
                    dimenPxSize(androidx.appcompat.R.dimen.abc_dialog_title_divider_material)
                layoutManager = verticalLayoutManager()
                adapter = InputMethodListAdapter(entries, enabledIndex) {
                    val (uniqueName, _, ime) = it
                    if (ime) service.switchInputMethod(uniqueName)
                    else service.lifecycleScope.launch { fcitx.activateIme(uniqueName) }
                    dialog.dismiss()
                }
                styledDrawable(androidx.appcompat.R.attr.dividerHorizontal)?.let {
                    addItemDecoration(SingleDividerDecoration(it, dividerIndex))
                }
            })
            .setNeutralButton(R.string.input_methods) { _, _ ->
                AppUtil.launchMainToConfig(service, MainActivity.INTENT_DATA_CONFIG_IM_LIST)
            }
            .create()
        dialog.window?.decorView
            ?.findViewById<Space>(androidx.appcompat.R.id.titleDividerNoCustom)?.apply {
                visibility = View.VISIBLE
            }
        return dialog
    }
}
