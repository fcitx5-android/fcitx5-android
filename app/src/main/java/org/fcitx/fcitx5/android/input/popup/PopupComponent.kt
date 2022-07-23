package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import java.util.*

class PopupComponent :
    UniqueComponent<PopupComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private val context by manager.context()
    private val theme by manager.theme()

    private val showingEntryUi = HashMap<Int, PopupEntryUi>()
    private val dismissJobs = HashMap<Int, Job>()
    private val freeEntryUi = LinkedList<PopupEntryUi>()

    private val showingKeyboardUi = HashMap<Int, PopupKeyboardUi>()

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

    val root by lazy {
        context.frameLayout {
            isClickable = false
            isFocusable = false
        }
    }

    fun showPopup(viewId: Int, content: String, bounds: Rect) {
        showingEntryUi[viewId]?.apply {
            dismissJobs[viewId]?.also {
                it.cancel()
                dismissJobs.remove(viewId)
            }
            lastShowTime = System.currentTimeMillis()
            setText(content)
            return
        }
        val popup = (freeEntryUi.poll() ?: PopupEntryUi(context, theme, popupRadius)).apply {
            lastShowTime = System.currentTimeMillis()
            setText(content)
        }
        root.apply {
            add(popup.root, lParams(popupWidth, popupHeight) {
                topMargin = bounds.bottom - popupHeight - keyBottomMargin
                leftMargin = (bounds.left + bounds.right - popupWidth) / 2
            })
        }
        showingEntryUi[viewId] = popup
    }

    fun showKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
        val popupEntryUi = showingEntryUi[viewId] ?: run {
            showPopup(viewId, "", bounds)
            showingEntryUi.getValue(viewId)
        }
        popupEntryUi.apply {
            val (x, y) = intArrayOf(0, 0).also { root.getLocationInWindow(it) }
            val keyboardUi = PopupKeyboardUi(ctx, theme, popupRadius, keyboard)
            showingKeyboardUi[viewId] = keyboardUi
            this@PopupComponent.root.apply {
                add(keyboardUi.root, lParams {
                    leftMargin = x
                    topMargin = y
                })
            }
        }
    }

    fun changeFocus(viewId: Int, deltaX: Int, deltaY: Int): Boolean {
        showingKeyboardUi[viewId]?.apply {
            moveFocus(deltaX, deltaY)
            return true
        }
        return false
    }

    fun triggerFocusedKeyboard(viewId: Int): KeyAction? {
        return showingKeyboardUi[viewId]?.trigger()
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
        showingKeyboardUi[viewId]?.also {
            showingKeyboardUi.remove(viewId)
            root.removeView(it.root)
        }
    }

    private fun reallyDismissPopup(viewId: Int, popup: PopupEntryUi) {
        showingEntryUi.remove(viewId)
        root.removeView(popup.root)
        freeEntryUi.add(popup)
    }

    fun dismissAll() {
        showingEntryUi.forEach {
            dismissJobs[it.key]?.apply {
                cancel()
                dismissJobs.remove(it.key)
            }
            reallyDismissPopup(it.key, it.value)
        }
    }
}
