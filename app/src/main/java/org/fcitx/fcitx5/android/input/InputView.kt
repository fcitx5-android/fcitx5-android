package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.Space
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeManager.Prefs.NavbarBackground
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.candidates.CandidateViewBuilder
import org.fcitx.fcitx5.android.input.candidates.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.wrapToUniqueComponent
import org.mechdancer.dependency.plusAssign
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageDrawable

@SuppressLint("ViewConstructor")
class InputView(
    val service: FcitxInputMethodService,
    val fcitx: Fcitx,
    val theme: Theme
) : ConstraintLayout(service) {

    private var shouldUpdateNavbarForeground = false
    private var shouldUpdateNavbarBackground = false

    private val keyBorder by ThemeManager.prefs.keyBorder
    private val navbarBackground by ThemeManager.prefs.navbarBackground

    private val customBackground = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val bottomPaddingSpace = view(::Space)

    private val themedContext = context.withTheme(R.style.Theme_FcitxAppTheme)

    private val broadcaster = InputBroadcaster()

    private val preedit = PreeditComponent()

    private val kawaiiBar = KawaiiBarComponent()

    private val horizontalCandidate = HorizontalCandidateComponent()

    private val keyboardWindow = KeyboardWindow()

    private val windowManager = InputWindowManager()

    private val candidateViewBuilder: CandidateViewBuilder = CandidateViewBuilder()

    private val commonKeyActionListener = CommonKeyActionListener()

    private val popup = PopupComponent()

    val scope = DynamicScope()

    private fun setupScope() {
        scope += service.wrapToUniqueComponent()
        scope += themedContext.wrapToUniqueComponent()
        scope += fcitx.wrapToUniqueComponent()
        scope += candidateViewBuilder
        scope += preedit
        scope += kawaiiBar
        scope += broadcaster
        scope += this.wrapToUniqueComponent()
        scope += windowManager
        scope += keyboardWindow
        scope += commonKeyActionListener
        scope += popup
        scope += horizontalCandidate
        scope += theme.wrapToUniqueComponent()
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

    val keyboardView: View

    init {
        // MUST call before any operation
        setupScope()

        AppPrefs.getInstance().keyboard.keyboardHeightPercent
            .registerOnChangeListener(onWindowHeightChangeListener)

        service.lifecycleScope.launch {
            broadcaster.onImeUpdate(fcitx.currentIme())
        }

        service.window.window!!.also {
            when (navbarBackground) {
                NavbarBackground.None -> {
                    WindowCompat.setDecorFitsSystemWindows(it, true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        it.isNavigationBarContrastEnforced = true
                    }
                }
                NavbarBackground.ColorOnly -> {
                    shouldUpdateNavbarForeground = true
                    shouldUpdateNavbarBackground = true
                    WindowCompat.setDecorFitsSystemWindows(it, true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        it.isNavigationBarContrastEnforced = false
                    }
                }
                NavbarBackground.Full -> {
                    shouldUpdateNavbarForeground = true
                    // allow draw behind navigation bar
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    // transparent navigation bar
                    it.navigationBarColor = Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // don't apply scrim to transparent navigation bar
                        it.isNavigationBarContrastEnforced = false
                    }
                    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                        insets.getInsets(WindowInsetsCompat.Type.navigationBars()).let {
                            bottomPaddingSpace.updateLayoutParams<LayoutParams> {
                                height = it.bottom
                            }
                        }
                        WindowInsetsCompat.CONSUMED
                    }
                }
            }
        }

        customBackground.imageDrawable = theme.backgroundDrawable(keyBorder)

        keyboardView = constraintLayout {
            add(customBackground, lParams {
                centerVertically()
                centerHorizontally()
            })
            add(kawaiiBar.view, lParams(matchParent, dp(40)) {
                topOfParent()
                centerHorizontally()
            })
            add(windowManager.view, lParams(matchParent, windowHeightPx) {
                below(kawaiiBar.view)
                centerHorizontally()
                above(bottomPaddingSpace)
            })
            add(bottomPaddingSpace, lParams(matchParent) {
                centerHorizontally()
                bottomOfParent()
            })
        }

        add(preedit.ui.root, lParams(matchParent, wrapContent) {
            above(keyboardView)
            centerHorizontally()
        })
        add(keyboardView, lParams(matchParent, wrapContent) {
            centerHorizontally()
            bottomOfParent()
        })
        add(popup.root, lParams(matchParent, matchParent) {
            centerVertically()
            centerHorizontally()
        })
    }

    private fun updateKeyboardHeight() {
        windowManager.view.updateLayoutParams {
            height = windowHeightPx
        }
    }

    override fun onDetachedFromWindow() {
        AppPrefs.getInstance().keyboard.keyboardHeightPercent
            .unregisterOnChangeListener(onWindowHeightChangeListener)
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
        super.onDetachedFromWindow()
    }

    fun onShow() {
        if (shouldUpdateNavbarForeground || shouldUpdateNavbarBackground) {
            service.window.window!!.also {
                if (shouldUpdateNavbarForeground) {
                    WindowCompat.getInsetsController(it, it.decorView)
                        .isAppearanceLightNavigationBars = !theme.isDark
                }
                if (shouldUpdateNavbarBackground) {
                    it.navigationBarColor = (when (theme) {
                        is Theme.Builtin -> if (keyBorder) theme.backgroundColor else theme.keyboardColor
                        is Theme.Custom -> theme.backgroundColor
                    }).color
                }
            }
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
                broadcaster.onPreeditUpdate(it.data)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                broadcaster.onInputPanelAuxUpdate(it.data)
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
