/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.broadcast.PunctuationComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import java.util.LinkedList

class PopupComponent :
    UniqueComponent<PopupComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private val context by manager.context()
    private val theme by manager.theme()
    private val punctuation: PunctuationComponent by manager.must()

    private val showingEntryUi = HashMap<Int, PopupEntryUi>()
    private val dismissJobs = HashMap<Int, Job>()
    private val freeEntryUi = LinkedList<PopupEntryUi>()

    private val showingContainerUi = HashMap<Int, PopupContainerUi>()

    private val keyBottomMargin by lazy {
        context.dp(ThemeManager.prefs.keyVerticalMargin.getValue())
    }
    private val popupWidth by lazy {
        context.dp(38)
    }
    private val popupHeight by lazy {
        context.dp(116)
    }
    private val popupKeyHeight by lazy {
        context.dp(48)
    }
    private val popupRadius by lazy {
        context.dp(ThemeManager.prefs.keyRadius.getValue()).toFloat()
    }
    private val hideThreshold = 100L

    private val rootLocation = intArrayOf(0, 0)
    private val rootBounds: Rect = Rect()

    val root by lazy {
        context.frameLayout {
            // we want (0, 0) at top left
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            isClickable = false
            isFocusable = false

            addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                val (x, y) = rootLocation.also { v.getLocationInWindow(it) }
                val width = right - left
                val height = bottom - top
                rootBounds.set(x, y, x + width, y + height)
            }
        }
    }

    private fun showPopup(
        viewId: Int,
        content: String,
        bounds: Rect,
        style: PopupAction.PreviewStyle
    ) {
        showingEntryUi[viewId]?.apply {
            dismissJobs[viewId]?.also {
                dismissJobs.remove(viewId)?.cancel()
            }
            lastShowTime = System.currentTimeMillis()
            setText(content)
            return
        }
        val popup = (freeEntryUi.poll()
            ?: PopupEntryUi(context, theme, popupKeyHeight, popupRadius)).apply {
            lastShowTime = System.currentTimeMillis()
            setText(content)
        }
        // translate absolute bounds into root-local coordinates
        val left = bounds.left - rootBounds.left
        val top = bounds.top - rootBounds.top
        val right = bounds.right - rootBounds.left
        val bottom = bounds.bottom - rootBounds.top

        val (w, h, lm, tm) = when (style) {
            PopupAction.PreviewStyle.Default -> {
                val w = popupWidth
                val h = popupHeight
                val lm = (left + right - w) / 2
                val tm = bottom - h - keyBottomMargin
                popup.setTextHeight(popupKeyHeight)
                LayoutSpec(w, h, lm, tm)
            }
            PopupAction.PreviewStyle.WideAbove -> {
                val keyW = right - left
                val keyH = bottom - top
                val widthScale = 1.2f
                val heightScale = 0.7f
                val w = (keyW * widthScale).toInt()
                val h = (keyH * heightScale).toInt()
                val centerX = (left + right) / 2
                val lmUnclamped = centerX - w / 2
                val gap = context.dp(4)
                val tmUnclamped = top - gap - h
                val rootW = rootBounds.width()
                val lm = lmUnclamped.coerceIn(0, rootW - w)
                val tm = tmUnclamped.coerceAtLeast(0)
                popup.setTextHeight(h)
                LayoutSpec(w, h, lm, tm)
            }
        }
        root.apply {
            add(popup.root, lParams(w, h) {
                leftMargin = lm
                topMargin = tm
            })
        }
        showingEntryUi[viewId] = popup
    }

    private fun updatePopup(viewId: Int, content: String) {
        showingEntryUi[viewId]?.setText(content)
    }

    private fun showKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
        val keys = PopupPreset[keyboard.label]
            ?: EmojiModifier.produceSkinTones(keyboard.label)
            ?: return
        // clear popup preview text         OR create empty popup preview
        showingEntryUi[viewId]?.setText("") ?: showPopup(viewId, "", bounds, PopupAction.PreviewStyle.Default)
        reallyShowKeyboard(viewId, keys, bounds)
    }

    private fun reallyShowKeyboard(viewId: Int, keys: Array<String>, bounds: Rect) {
        val labels = if (punctuation.enabled) {
            Array(keys.size) { punctuation.transform(keys[it]) }
        } else keys
        val keyboardUi = PopupKeyboardUi(
            context,
            theme,
            rootBounds,
            bounds,
            { dismissPopup(viewId) },
            popupRadius,
            popupWidth,
            popupKeyHeight,
            // position popup keyboard higher, because of [^1]
            popupHeight + keyBottomMargin,
            keys,
            labels
        )
        showPopupContainer(viewId, keyboardUi)
    }

    private fun showMenu(viewId: Int, menu: KeyDef.Popup.Menu, bounds: Rect) {
        showingEntryUi[viewId]?.let {
            dismissPopupEntry(viewId, it)
        }
        val menuUi = PopupMenuUi(
            context,
            theme,
            rootBounds,
            bounds,
            { dismissPopup(viewId) },
            menu.items,
        )
        showPopupContainer(viewId, menuUi)
    }

    private fun showPopupContainer(viewId: Int, ui: PopupContainerUi) {
        // Remove any existing container for this viewId to avoid stacking/leaks
        showingContainerUi[viewId]?.also { old ->
            root.removeView(old.root)
        }
        root.apply {
            add(ui.root, lParams {
                leftMargin = ui.triggerBounds.left + ui.offsetX - rootBounds.left
                topMargin = ui.triggerBounds.top + ui.offsetY - rootBounds.top
            })
        }
        showingContainerUi[viewId] = ui
    }

    private fun changeFocus(viewId: Int, x: Float, y: Float): Boolean {
        return showingContainerUi[viewId]?.changeFocus(x, y) ?: false
    }

    private fun triggerFocused(viewId: Int): KeyAction? {
        return showingContainerUi[viewId]?.onTrigger()
    }

    private fun dismissPopup(viewId: Int) {
        dismissPopupContainer(viewId)
        showingEntryUi[viewId]?.also {
            val timeLeft = it.lastShowTime + hideThreshold - System.currentTimeMillis()
            if (timeLeft <= 0L) {
                dismissPopupEntry(viewId, it)
            } else {
                dismissJobs[viewId] = service.lifecycleScope.launch {
                    delay(timeLeft)
                    dismissPopupEntry(viewId, it)
                    dismissJobs.remove(viewId)
                }
            }
        }
    }

    private fun dismissPopupContainer(viewId: Int) {
        showingContainerUi[viewId]?.also {
            showingContainerUi.remove(viewId)
            root.removeView(it.root)
        }
    }

    private fun dismissPopupEntry(viewId: Int, popup: PopupEntryUi) {
        showingEntryUi.remove(viewId)
        root.removeView(popup.root)
        popup.resetForReuse()
        freeEntryUi.add(popup)
    }

    fun dismissAll() {
        // avoid modifying collection while iterating
        dismissJobs.forEach { (_, job) ->
            job.cancel()
        }
        dismissJobs.clear()
        // too
        showingContainerUi.forEach { (_, container) ->
            root.removeView(container.root)
        }
        showingContainerUi.clear()
        // too too
        showingEntryUi.forEach { (_, entry) ->
            root.removeView(entry.root)
            freeEntryUi.add(entry)
        }
        showingEntryUi.clear()
    }

    val listener = PopupActionListener { action ->
        with(action) {
            when (this) {
                is PopupAction.ChangeFocusAction -> outResult = changeFocus(viewId, x, y)
                is PopupAction.DismissAction -> dismissPopup(viewId)
                is PopupAction.PreviewAction -> showPopup(viewId, content, bounds, style)
                is PopupAction.PreviewUpdateAction -> updatePopup(viewId, content)
                is PopupAction.PreviewDangerHintAction -> showingEntryUi[viewId]?.setDangerHint(danger)
                is PopupAction.ShowKeyboardAction -> showKeyboard(viewId, keyboard, bounds)
                is PopupAction.ShowMenuAction -> showMenu(viewId, menu, bounds)
                is PopupAction.ShowClearConfirmAction -> {
                    val existing = showingContainerUi[viewId]
                    if (existing is PopupClearUi) {
                        existing.setDangerHint(danger)
                    } else {
                        val ui = PopupClearUi(
                            context,
                            theme,
                            rootBounds,
                            bounds,
                            danger,
                        ) { dismissPopup(viewId) }
                        showPopupContainer(viewId, ui)
                    }
                }
                is PopupAction.TriggerAction -> outAction = triggerFocused(viewId)
            }
        }
    }
}

private data class LayoutSpec(val width: Int, val height: Int, val leftMargin: Int, val topMargin: Int)
