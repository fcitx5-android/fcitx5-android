package org.fcitx.fcitx5.android.input.picker

import androidx.transition.Fade
import androidx.transition.Transition
import com.google.android.material.tabs.TabLayoutMediator
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

class PickerWindow() :
    InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow, InputBroadcastReceiver {

    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()

    companion object : EssentialWindow.Key

    override val key: EssentialWindow.Key
        get() = PickerWindow

    override val enterAnimation: Transition
        get() = Fade()

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter
    private lateinit var tabLayoutMediator: TabLayoutMediator

    private val keyActionListener = KeyActionListener { it, source ->
        when (it) {
            is KeyAction.LayoutSwitchAction -> when (it.act) {
                NumberKeyboard.Name -> {
                    // TODO: switch to NumberKeyboard directly
                    windowManager.attachWindow(KeyboardWindow)
                }
                else -> {
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
        pickerPagesAdapter = PickerPagesAdapter(theme)
        pager.adapter = pickerPagesAdapter
        // TODO: show tabs for symbol categories, not pages
        tabLayoutMediator = TabLayoutMediator(tab, pager) { tab, position ->
            tab.text = "$position"
        }.apply { attach() }
    }

    override fun onCreateBarExtension() = pickerLayout.tab

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.keyActionListener = keyActionListener
    }

    override fun onDetached() {
        pickerLayout.embeddedKeyboard.keyActionListener = null
    }

    override val showTitle = false
}