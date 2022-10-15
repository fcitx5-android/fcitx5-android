package org.fcitx.fcitx5.android.input.candidates

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.ContextThemeWrapper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.candidates.adapter.GridCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.adapter.SimpleCandidateViewAdapter
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp

class CandidateViewBuilder : UniqueComponent<CandidateViewBuilder>(), Dependent,
    ManagedHandler by managedHandler() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val context: ContextThemeWrapper by manager.context()
    private val fcitx: FcitxConnection by manager.fcitx()
    private val theme by manager.theme()

    fun gridAdapter() = object : GridCandidateViewAdapter() {
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.runOnReady { select(idx) } }
        }

        override val theme: Theme
            get() = this@CandidateViewBuilder.theme
    }

    fun simpleAdapter() = object : SimpleCandidateViewAdapter() {
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.runOnReady { select(idx) } }
        }

        override val theme: Theme
            get() = this@CandidateViewBuilder.theme
    }

    fun dividerDrawable() = ShapeDrawable(RectShape()).apply {
        intrinsicWidth = context.dp(1)
        intrinsicHeight = context.dp(1)
        paint.color = theme.dividerColor
    }
}