package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import cn.berberman.girls.utils.either.otherwise
import cn.berberman.girls.utils.either.then
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.UniqueComponentWrapper
import org.mechdancer.dependency.plusAssign
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.bottomPadding
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

    val scope = DynamicScope()

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

    private val windowHeightPercent: Int by AppPrefs.getInstance().keyboard.keyboardHeightPercent
    private val windowHeightPercentLandscape: Int by AppPrefs.getInstance().keyboard.keyboardHeightPercentLandscape

    private val windowHeightPx: Int
        get() {
            val percent = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> windowHeightPercentLandscape
                else -> windowHeightPercent
            }
            return resources.displayMetrics.heightPixels * percent / 100
        }

    private val onWindowHeightChangeListener = ManagedPreference.OnChangeListener<Int> {
        updateKeyboardHeight()
    }

    init {
        // MUST call before any operation
        setupScope()

        service.window.window!!.also {
            // allow draw behind navigation bar
            WindowCompat.setDecorFitsSystemWindows(it, false)
            // transparent navigation bar
            it.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // don't apply scrim to transparent navigation bar
                it.isNavigationBarContrastEnforced = false
            }
        }

        AppPrefs.getInstance().keyboard.keyboardHeightPercent
            .registerOnChangeListener(onWindowHeightChangeListener)
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).let {
                v.bottomPadding = it.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        service.lifecycleScope.launch {
            broadcaster.onImeUpdate(fcitx.currentIme())
        }

        ThemeManager.currentTheme.background.then {
            background =
                BitmapDrawable(resources, BitmapFactory.decodeFile(it.path))
        }
            .otherwise {
                backgroundColor = it.resolve(themedContext)
            }

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

    private fun updateKeyboardHeight() {
        windowManager.view.updateLayoutParams {
            height = windowHeightPx
        }
    }

    override fun onDetachedFromWindow() {
        preedit.dismiss()
        AppPrefs.getInstance().keyboard.keyboardHeightPercent
            .unregisterOnChangeListener(onWindowHeightChangeListener)
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
        super.onDetachedFromWindow()
    }

    fun onShow() {
        service.window.window?.also {
            ViewCompat.getWindowInsetsController(it.decorView)?.isAppearanceLightNavigationBars =
                ThemeManager.currentTheme.lightNavigationBar.resolve(themedContext)
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
            is FcitxEvent.StatusAreaEvent -> {
                broadcaster.onStatusAreaUpdate(it.data)
            }
            else -> {
            }
        }
    }

    fun onSelectionUpdate(start: Int, end: Int) {
        broadcaster.onSelectionUpdate(start, end)
    }

}
