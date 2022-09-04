package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import androidx.core.content.ContextCompat
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
        val keys = PopupPreset[keyboard.label] ?: return
        val entryUi = showingEntryUi[viewId]
        if (entryUi != null) {
            reallyShowKeyboard(viewId, entryUi, keys, bounds)
        } else {
            showPopup(viewId, keyboard.label, bounds)
            // in case popup preview is disabled, wait newly created popup entry to layout
            ContextCompat.getMainExecutor(service).execute {
                reallyShowKeyboard(viewId, showingEntryUi.getValue(viewId), keys, bounds)
            }
        }
    }

    private fun reallyShowKeyboard(
        viewId: Int,
        entryUi: PopupEntryUi,
        keys: Array<String>,
        bounds: Rect
    ) {
        val keyboardUi = PopupKeyboardUi(context, theme, popupRadius, keys, bounds)
        val (x, y) = intArrayOf(0, 0).also { entryUi.root.getLocationInWindow(it) }
        root.apply {
            add(keyboardUi.root, lParams {
                leftMargin = x + keyboardUi.offsetX
                topMargin = y + keyboardUi.offsetY
            })
        }
        showingKeyboardUi[viewId] = keyboardUi
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
    }

    private fun reallyDismissPopup(viewId: Int, popup: PopupEntryUi) {
        showingKeyboardUi[viewId]?.also {
            showingKeyboardUi.remove(viewId)
            root.removeView(it.root)
        }
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
