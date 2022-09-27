package org.fcitx.fcitx5.android.input.picker

import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.transition.Slide
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

class PickerWindow(val data: List<Pair<String, Array<String>>>) :
    InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow, InputBroadcastReceiver {

    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()

    companion object : EssentialWindow.Key

    override val key: EssentialWindow.Key
        get() = PickerWindow

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }.takeIf {
        // disable animation switching between keyboard
        lastWindow !is KeyboardWindow
    }

    override fun exitAnimation(nextWindow: InputWindow) = super.exitAnimation(nextWindow).takeIf {
        // disable animation switching between keyboard
        nextWindow !is KeyboardWindow
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
            else -> {
                commonKeyActionListener.listener.onKeyAction(it, source)
            }
        }
    }

    override fun onCreateView() = PickerLayout(context, theme).apply {
        pickerLayout = this
        pickerPagesAdapter = PickerPagesAdapter(theme, keyActionListener, data)
        tabsUi.apply {
            setTabs(pickerPagesAdapter.categories)
            setOnTabClickListener { i ->
                pager.setCurrentItem(pickerPagesAdapter.getStartPageOfCategory(i), false)
            }
        }
        pager.apply {
            adapter = pickerPagesAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tabsUi.activateTab(pickerPagesAdapter.getCategoryOfPage(position))
                }
            })
            // show first symbol category by default, rather than recently used
            setCurrentItem(pickerPagesAdapter.getStartPageOfCategory(1), false)
        }
    }

    override fun onCreateBarExtension() = pickerLayout.tabsUi.root

    override fun beforeAttached() {
        pickerPagesAdapter.updateRecent()
    }

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.keyActionListener = keyActionListener
    }

    override fun onDetached() {
        pickerLayout.embeddedKeyboard.keyActionListener = null
        pickerPagesAdapter.saveRecent()
    }

    override val showTitle = false
}