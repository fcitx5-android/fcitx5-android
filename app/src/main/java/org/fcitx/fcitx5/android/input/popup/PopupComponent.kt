package org.fcitx.fcitx5.android.input.popup

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import timber.log.Timber
import java.util.*

class PopupComponent :
    UniqueComponent<PopupComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private val context by manager.context()
    private val theme by manager.theme()

    private val showingEntryUi = HashMap<Int, PopupEntryUi>()
    private val dismissJobs = HashMap<Int, Job>()
    private val freeEntryUi = LinkedList<PopupEntryUi>()

    private val keyBottomMargin by lazy {
        context.dp(ThemeManager.prefs.keyVerticalMargin.getValue())
    }
    private val popupWidth by lazy {
        context.dp(38)
    }
    private val popupHeight by lazy {
        context.dp(116)
    }
    private val popupRadius by lazy {
        context.dp(ThemeManager.prefs.keyRadius.getValue()).toFloat()
    }
    private val hideThreshold = 100L

    val view by lazy {
        context.frameLayout {
            isClickable = false
            isFocusable = false
        }
    }

    fun showPopup(viewId: Int, character: String, left: Int, top: Int, right: Int, bottom: Int) {
        Timber.d("showPopup('$character', left=$left, top=$top, right=$right, bottom=$bottom)")
        showingEntryUi[viewId]?.apply {
            dismissJobs[viewId]?.also {
                it.cancel()
                dismissJobs.remove(viewId)
            }
            lastShowTime = System.currentTimeMillis()
            setText(character)
            return
        }
        val popup = (freeEntryUi.poll() ?: PopupEntryUi(context, theme, popupRadius)).apply {
            lastShowTime = System.currentTimeMillis()
            setText(character)
        }
        view.apply {
            add(popup.root, lParams(popupWidth, popupHeight) {
                topMargin = bottom - popupHeight - keyBottomMargin
                leftMargin = (left + right - popupWidth) / 2
            })
        }
        showingEntryUi[viewId] = popup
    }

    fun dismissPopup(viewId: Int) {
        showingEntryUi[viewId]?.also {
            val timeLeft = it.lastShowTime + hideThreshold - System.currentTimeMillis()
            if (timeLeft <= 0L) {
                reallyDismissPopup(viewId, it)
            } else {
                dismissJobs[viewId] = service.lifecycleScope.launch {
                    delay(timeLeft)
                    reallyDismissPopup(viewId, it)
                    dismissJobs.remove(viewId)
                }
            }
        }
    }

    private fun reallyDismissPopup(viewId: Int, popup: PopupEntryUi) {
        showingEntryUi.remove(viewId)
        view.removeView(popup.root)
        freeEntryUi.add(popup)
    }
}
