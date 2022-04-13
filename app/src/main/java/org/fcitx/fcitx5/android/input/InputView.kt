package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.OrientationEventListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.UniqueComponentWrapper
import org.mechdancer.dependency.plusAssign
import org.mechdancer.dependency.scope
import splitties.dimensions.dp
import splitties.resources.color
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.withTheme


@SuppressLint("ViewConstructor")
class InputView(
    val service: FcitxInputMethodService,
    val fcitx: Fcitx
) : ConstraintLayout(service) {

    private val themedContext = context.withTheme(R.style.Theme_FcitxAppTheme)

    private val broadcaster = InputBroadcaster()

    private val preedit = PreeditComponent()

    private val kawaiiBar = KawaiiBarComponent()

    private val horizontalCandidate = HorizontalCandidateComponent()

    private val keyboardWindow = KeyboardWindow()

    private val windowManager = InputWindowManager()

    private val candidateViewBuilder: CandidateViewBuilder = CandidateViewBuilder()

    private val commonKeyActionListener = CommonKeyActionListener()

    val scope = scope { }

    private fun setupScope() {
        scope += UniqueComponentWrapper(service)
        scope += UniqueComponentWrapper(themedContext)
        scope += UniqueComponentWrapper(fcitx)
        scope += candidateViewBuilder
        scope += preedit
        scope += kawaiiBar
        scope += broadcaster
        scope += UniqueComponentWrapper(this)
        scope += windowManager
        scope += keyboardWindow
        scope += commonKeyActionListener
        scope += horizontalCandidate
        broadcaster.onScopeSetupFinished(scope)
    }

    private val windowHeightPercent: Int by Prefs.getInstance().keyboardHeightPercent
    private val windowHeightPercentLandscape: Int by Prefs.getInstance().keyboardHeightPercentLandscape

    private val windowHeightPx: Int
        get() {
            val percent = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> windowHeightPercentLandscape
                else -> windowHeightPercent
            }
            return resources.displayMetrics.heightPixels * percent / 100
        }

    private val onWindowHeightChangeListener = Prefs.OnChangeListener<Int> {
        updateKeyboardHeight()
    }

    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(o: Int) = updateKeyboardHeight()
    }

    init {
        // MUST call before any operation
        setupScope()

        Prefs.getInstance().keyboardHeightPercent
            .registerOnChangeListener(onWindowHeightChangeListener)
        orientationListener.enable()

        service.lifecycleScope.launch {
            broadcaster.onImeUpdate(fcitx.currentIme())
        }
        backgroundColor = themedContext.styledColor(android.R.attr.colorBackground)
        add(kawaiiBar.view, lParams(matchParent, dp(40)) {
            topOfParent()
            startOfParent()
            endOfParent()
        })
        add(windowManager.view, lParams(matchParent, windowHeightPx) {
            below(kawaiiBar.view)
            startOfParent()
            endOfParent()
            bottomOfParent()
        })
    }

    fun updateKeyboardHeight() {
        windowManager.view.updateLayoutParams {
            height = windowHeightPx
        }
    }

    override fun onDetachedFromWindow() {
        preedit.dismiss()
        Prefs.getInstance().keyboardHeightPercent
            .unregisterOnChangeListener(onWindowHeightChangeListener)
        orientationListener.disable()
        super.onDetachedFromWindow()
    }

    fun onShow() {
        service.window.window?.also {
            val bkgColor = themedContext.styledColor(android.R.attr.colorBackground)
            it.navigationBarColor = bkgColor
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightNavigationBars =
                ColorUtils.calculateContrast(color(android.R.color.white), bkgColor) < 1.5f
        }
        kawaiiBar.onShow()
        windowManager.switchToKeyboardWindow()
        broadcaster.onEditorInfoUpdate(service.editorInfo)
    }

    fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                broadcaster.onCandidateUpdate(it.data)
            }
            is FcitxEvent.PreeditEvent -> {
                preedit.updatePreedit(it)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                preedit.updateAux(it)
            }
            is FcitxEvent.IMChangeEvent -> {
                broadcaster.onImeUpdate(it.data)
            }
            else -> {
            }
        }
    }

    fun onSelectionUpdate(start: Int, end: Int) {
        broadcaster.onSelectionUpdate(start, end)
    }

}
