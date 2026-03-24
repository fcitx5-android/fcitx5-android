/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.transition.Transition
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
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
    private val data: List<Pair<PickerData.Category, Array<String>>>,
    private val density: PickerPageUi.Density,
    private val switchKey: KeyDef,
    private val popupPreview: Boolean = true,
    private val followKeyBorder: Boolean = true
) : InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow {

    enum class Key : EssentialWindow.Key {
        Symbol,
        Emoji,
        Emoticon
    }

    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()
    private val returnKeyDrawable: ReturnKeyDrawableComponent by manager.must()

    private val keyBorder by ThemeManager.prefs.keyBorder

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter

    override fun enterAnimation(lastWindow: InputWindow): Transition? = null

    override fun exitAnimation(nextWindow: InputWindow): Transition? = null

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

    private val isEmoji = key === Key.Emoji

    override fun onCreateView() = PickerLayout(context, theme, switchKey).apply {
        pickerLayout = this
        val bordered = followKeyBorder && keyBorder
        pickerPagesAdapter = PickerPagesAdapter(
            theme, keyActionListener, popupActionListener, data,
            density, key.name, bordered, isEmoji
        )
        tabsUi.apply {
            setTabs(pickerPagesAdapter.getCategoryList())
            setOnTabClickListener { i ->
                pager.setCurrentItem(pickerPagesAdapter.getRangeOfCategoryIndex(i).first, false)
            }
        }
        pager.apply {
            adapter = pickerPagesAdapter
            // show first symbol category by default, rather than recently used
            val range = pickerPagesAdapter.getRangeOfCategoryIndex(1)
            setCurrentItem(range.first, false)
            // update initial tab and page manually to avoid
            // "Adding or removing callbacks during dispatch to callbacks"
            tabsUi.activateTab(1)
            paginationUi.updatePageCount(range.run { last - first + 1 })
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    val range = pickerPagesAdapter.getCategoryRangeOfPage(position)
                    paginationUi.updatePageCount(range.run { last - first + 1 })
                    paginationUi.updateScrollProgress(position - range.first, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    tabsUi.activateTab(pickerPagesAdapter.getCategoryIndexOfPage(position))
                    popup.dismissAll()
                }
            })
        }
    }

    override fun onCreateBarExtension() = pickerLayout.tabsUi.root

    val symbolPrefs = AppPrefs.getInstance().symbols
    private val hideUnsupportedEmojisPrefs = symbolPrefs.hideUnsupportedEmojis
    private val defaultEmojiSkinTonePrefs = symbolPrefs.defaultEmojiSkinTone

    @SuppressLint("NotifyDataSetChanged")
    private val initDataListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        pickerPagesAdapter.rebuildCategories(hideUnsupportedEmojisPrefs.getValue())
        pickerPagesAdapter.notifyDataSetChanged()
    }.takeIf { isEmoji }

    @SuppressLint("NotifyDataSetChanged")
    private val refreshPagesListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        pickerPagesAdapter.notifyDataSetChanged()
    }

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.also {
            it.onReturnDrawableUpdate(returnKeyDrawable.resourceId)
            it.keyActionListener = keyActionListener
        }
        if (isEmoji) {
            hideUnsupportedEmojisPrefs.registerOnChangeListener(initDataListener!!)
            defaultEmojiSkinTonePrefs.registerOnChangeListener(refreshPagesListener)
        }
    }

    override fun onDetached() {
        popup.dismissAll()
        pickerLayout.embeddedKeyboard.keyActionListener = null
        if (isEmoji) {
            hideUnsupportedEmojisPrefs.unregisterOnChangeListener(initDataListener!!)
            defaultEmojiSkinTonePrefs.unregisterOnChangeListener(refreshPagesListener)
        }
    }

    override val showTitle = false
}