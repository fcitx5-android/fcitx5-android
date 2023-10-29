/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

class PickerWindow(
    override val key: Key,
    val data: List<Pair<PickerData.Category, Array<String>>>,
    val density: PickerPageUi.Density,
    private val switchKey: KeyDef,
    val popupPreview: Boolean = true
) : InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow {

    enum class Key : EssentialWindow.Key {
        Symbol,
        Emoji,
        Emoticon
    }

    private val service by manager.inputMethodService()
    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()
    private val returnKeyDrawable: ReturnKeyDrawableComponent by manager.must()

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter

    override fun enterAnimation(lastWindow: InputWindow): Transition? {
        // disable animation switching between keyboard
        return if (lastWindow !is KeyboardWindow && lastWindow !is PickerWindow)
            Slide().apply { slideEdge = Gravity.BOTTOM }
        else null
    }

    override fun exitAnimation(nextWindow: InputWindow): Transition? {
        // disable animation switching between keyboard
        return if (nextWindow !is KeyboardWindow && nextWindow !is PickerWindow)
            super.exitAnimation(nextWindow)
        else null
    }

    private val keyActionListener = KeyActionListener { it, source ->
        when (it) {
            is KeyAction.LayoutSwitchAction -> {
                // Switch to NumberKeyboard before attaching KeyboardWindow
                (windowManager.getEssentialWindow(KeyboardWindow) as KeyboardWindow)
                    .switchLayout(it.act)
                // The real switchLayout (detachCurrentLayout and attachLayout) in KeyboardWindow is postponed,
                // so we have to postpone attachWindow as well
                ContextCompat.getMainExecutor(context).execute {
                    windowManager.attachWindow(KeyboardWindow)
                }
            }

            is KeyAction.FcitxKeyAction -> {
                // we want the behavior of CommitAction (commit the character as-is),
                // but don't want to include it in recently used list
                commonKeyActionListener.listener.onKeyAction(KeyAction.CommitAction(it.act), source)
            }

            else -> {
                if (it is KeyAction.CommitAction) {
                    pickerPagesAdapter.insertRecent(it.text)
                }
                commonKeyActionListener.listener.onKeyAction(it, source)
            }
        }
    }

    private val popupActionListener: PopupActionListener by lazy {
        PopupActionListener {
            when (it) {
                is PopupAction.PreviewAction -> {
                    if (!popupPreview) return@PopupActionListener
                }
                is PopupAction.ShowKeyboardAction -> {
                    // prevent ViewPager from consuming swipe gesture when popup keyboard shown
                    pickerLayout.pager.isUserInputEnabled = false
                }
                is PopupAction.DismissAction -> {
                    // restore ViewPager scrolling
                    pickerLayout.pager.isUserInputEnabled = true
                }
                else -> {}
            }
            popup.listener.onPopupAction(it)
        }
    }

    override fun onCreateView() = PickerLayout(context, theme, switchKey).apply {
        pickerLayout = this
        pickerPagesAdapter = PickerPagesAdapter(
            theme, keyActionListener, popupActionListener, data, density, key.name
        )
        tabsUi.apply {
            setTabs(pickerPagesAdapter.categories)
            setOnTabClickListener { i ->
                pager.setCurrentItem(pickerPagesAdapter.getStartPageOfCategory(i), false)
            }
        }
        pager.apply {
            adapter = pickerPagesAdapter
            // show first symbol category by default, rather than recently used
            val initialPage = pickerPagesAdapter.getStartPageOfCategory(1)
            setCurrentItem(initialPage, false)
            // update initial tab and page manually to avoid
            // "Adding or removing callbacks during dispatch to callbacks"
            tabsUi.activateTab(1)
            paginationUi.updatePageCount(
                pickerPagesAdapter.getCategoryRangeOfPage(initialPage).run { last - first + 1 }
            )
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    val range = pickerPagesAdapter.getCategoryRangeOfPage(position)
                    val start = range.first
                    val total = range.last - start + 1
                    val current = position - start
                    paginationUi.updatePageCount(total)
                    paginationUi.updateScrollProgress(current, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    tabsUi.activateTab(pickerPagesAdapter.getCategoryOfPage(position))
                    popup.dismissAll()
                }
            })
        }
    }

    override fun onCreateBarExtension() = pickerLayout.tabsUi.root

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.also {
            it.onReturnDrawableUpdate(returnKeyDrawable.resourceId)
            it.keyActionListener = keyActionListener
        }
    }

    override fun onDetached() {
        popup.dismissAll()
        pickerLayout.embeddedKeyboard.keyActionListener = null
        service.lifecycleScope.launch(Dispatchers.IO) {
            pickerPagesAdapter.saveRecent()
        }
    }

    override val showTitle = false
}