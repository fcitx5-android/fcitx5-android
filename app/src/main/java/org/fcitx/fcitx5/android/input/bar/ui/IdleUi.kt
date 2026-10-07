/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ViewAnimator
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.bar.ui.idle.ButtonsBarUi
import org.fcitx.fcitx5.android.input.bar.ui.idle.ClipboardSuggestionUi
import org.fcitx.fcitx5.android.input.bar.ui.idle.InlineSuggestionsUi
import org.fcitx.fcitx5.android.input.bar.ui.idle.NumberRow
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.voice.speech.SoundWaveView
import org.fcitx.fcitx5.android.input.voice.speech.SpeechRecognizerComponent
import kotlin.math.roundToInt
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber

class IdleUi(
    override val ctx: Context,
    private val theme: Theme,
    private val popup: PopupComponent,
    private val commonKeyActionListener: CommonKeyActionListener
) : Ui {

    enum class State {
        Empty, Toolbar, Clipboard, NumberRow, InlineSuggestion
    }

    var currentState = State.Empty
        private set

    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private var inPrivate = false

    private val translateDirection by lazy {
        if (ctx.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) 1f else -1f
    }

    private val menuButtonRotation
        get() = when {
            inPrivate -> 0f
            currentState == State.Toolbar -> 90f * translateDirection
            else -> -90f * translateDirection
        }

    val menuButton = ToolButton(ctx, R.drawable.ic_baseline_expand_more_24, theme).apply {
        iconRotation = menuButtonRotation
    }

    val hideKeyboardButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_drop_down_24, theme)

    // SpeechRecognizer sessions drive this sound wave while listening; it is only
    // visible during an active voice input session, otherwise the bar stays empty.
    val soundWave = SoundWaveView(ctx).apply {
        enableIdle(true)
        setColor(theme.altKeyTextColor)
        maxVolume = 12
        minVolume = 2
        minVolumeBarHeight = dp(3)
        maxVolumeBarHeight = dp(KawaiiBarComponent.HEIGHT - 6)
        maxIdleHeight = maxVolumeBarHeight / 2
        visibility = View.GONE
    }

    val emptyBar = android.widget.FrameLayout(ctx).apply {
        add(soundWave, lParams(matchParent, matchParent))
    }

    val audioVolumeListener = SpeechRecognizerComponent.AudioVolumeListener { active, level ->
        // The wave is visible ONLY while the service actually reports audio levels:
        // services that never call rmsChanged (e.g. Sayboard) would otherwise show
        // a dead wave. Visibility follows the volume events instead of the session
        // state; the teardown path always reports active=false, hiding it again.
        soundWave.visibility = if (active) View.VISIBLE else View.GONE
        // below ~-50 dB the wave falls back to the idle animation, so silence and speech
        // are visually distinct
        if (active && level > 0.04f) {
            soundWave.handleVolume(
                (soundWave.minVolume + level * (soundWave.maxVolume - soundWave.minVolume)).roundToInt()
            )
        } else {
            soundWave.stopDance()
        }
    }

    val buttonsUi = ButtonsBarUi(ctx, theme)

    val clipboardUi = ClipboardSuggestionUi(ctx, theme)

    val numberRow = NumberRow(ctx, theme).apply {
        visibility = View.GONE
    }

    val inlineSuggestionsBar = InlineSuggestionsUi(ctx)

    private val animator = ViewAnimator(ctx).apply {
        add(emptyBar, lParams(matchParent, matchParent))
        add(buttonsUi.root, lParams(matchParent, matchParent))
        add(clipboardUi.root, lParams(matchParent, matchParent))
        add(inlineSuggestionsBar.root, lParams(matchParent, matchParent))
    }

    private val inAnimation by lazy {
        AnimationSet(true).apply {
            duration = 200L
            addAnimation(AlphaAnimation(0f, 1f))
            // 2 stands for Animation.RELATIVE_TO_PARENT
            addAnimation(TranslateAnimation(2, -0.3f * translateDirection, 2, 0f, 0, 0f, 0, 0f))
        }
    }

    private val outAnimation by lazy {
        AnimationSet(true).apply {
            duration = 200L
            addAnimation(AlphaAnimation(1f, 0f))
            addAnimation(TranslateAnimation(2, 0f, 2, -0.3f * translateDirection, 0, 0f, 0, 0f))
        }
    }

    private val idleBody = constraintLayout {
        val size = dp(KawaiiBarComponent.HEIGHT)
        add(menuButton, lParams(size, size) {
            startOfParent()
            centerVertically()
        })
        add(hideKeyboardButton, lParams(size, size) {
            endOfParent()
            centerVertically()
        })
        add(animator, lParams(matchConstraints, matchParent) {
            after(menuButton)
            before(hideKeyboardButton)
            centerVertically()
        })
    }

    override val root = frameLayout {
        add(idleBody, lParams(matchParent, matchParent))
        add(numberRow, lParams(matchParent, matchParent))
    }

    fun privateMode(activate: Boolean = true) {
        if (activate == inPrivate) return
        inPrivate = activate
        updateMenuButtonIcon()
        updateMenuButtonContentDescription()
        updateMenuButtonRotation(instant = true)
    }

    private fun updateMenuButtonIcon() {
        menuButton.setIcon(
            if (inPrivate) R.drawable.ic_view_private
            else R.drawable.ic_baseline_expand_more_24
        )
    }

    private fun updateMenuButtonContentDescription() {
        menuButton.contentDescription = when {
            inPrivate -> ctx.getString(R.string.private_mode)
            currentState == State.Toolbar -> ctx.getString(R.string.hide_toolbar)
            else -> ctx.getString(R.string.expand_toolbar)
        }
    }

    private fun updateMenuButtonRotation(instant: Boolean = false) {
        val targetRotation = menuButtonRotation
        menuButton.apply {
            if (targetRotation == iconRotation) return
            iconAnimate().cancel()
            if (!instant && !disableAnimation) {
                iconAnimate().setDuration(200L).rotation(targetRotation)
            } else {
                iconRotation = targetRotation
            }
        }
    }

    private var hideKeyboardIsVoiceInput = false
    private var voiceInputActive = false

    /** Wired when hold-to-talk is enabled; null restores normal tap handling. */
    private var holdToTalkHandler: VoiceInputHoldHandler? = null
    private var holdToTalkDown = false
    private var holdToTalkDownTime = 0L

    /**
     * Hold-to-talk behavior of the toolbar microphone button: press starts a
     * recognition session, release stops it (final text arrives on release).
     */
    interface VoiceInputHoldHandler {
        fun onHoldStart()
        fun onHoldStop()
        fun onHoldCancel()
    }

    private val holdToTalkTouchListener = View.OnTouchListener { _, e ->
        val handler = holdToTalkHandler ?: return@OnTouchListener false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                holdToTalkDown = true
                holdToTalkDownTime = e.downTime
                handler.onHoldStart()
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (holdToTalkDown) {
                    holdToTalkDown = false
                    val action = HoldToTalkGesture.releaseAction(
                        e.eventTime - holdToTalkDownTime,
                        e.actionMasked == MotionEvent.ACTION_CANCEL
                    )
                    when (action) {
                        HoldToTalkGesture.ReleaseAction.Stop -> handler.onHoldStop()
                        HoldToTalkGesture.ReleaseAction.Cancel -> handler.onHoldCancel()
                    }
                }
                true
            }
            else -> true
        }
    }

    fun setHideKeyboardIsVoiceInput(
        isVoiceInput: Boolean,
        callback: View.OnClickListener,
        holdHandler: VoiceInputHoldHandler? = null
    ) {
        hideKeyboardIsVoiceInput = isVoiceInput
        holdToTalkHandler = if (isVoiceInput) holdHandler else null
        holdToTalkDown = false
        if (isVoiceInput) {
            hideKeyboardButton.setIcon(R.drawable.ic_baseline_keyboard_voice_24)
            applyVoiceInputActiveState()
            if (holdHandler != null) {
                // the touch listener consumes all events while it is active, so the
                // tap callback would never run; detach it explicitly
                hideKeyboardButton.setOnClickListener(null)
                hideKeyboardButton.setOnTouchListener(holdToTalkTouchListener)
            } else {
                hideKeyboardButton.setOnTouchListener(null)
                hideKeyboardButton.setOnClickListener(callback)
            }
        } else {
            hideKeyboardButton.setOnTouchListener(null)
            stopRecordingIndicators()
            hideKeyboardButton.setIcon(R.drawable.ic_baseline_arrow_drop_down_24)
            hideKeyboardButton.contentDescription = ctx.getString(R.string.hide_keyboard)
            hideKeyboardButton.setOnClickListener(callback)
        }
    }

    private fun stopRecordingIndicators() {
        hideKeyboardButton.stopRecordingIndicator(
            theme.altKeyTextColor,
            theme.keyPressHighlightColor
        )
    }

    /**
     * Highlights the microphone button while a recognition session is running: the icon is
     * tinted with the accent color and pulses, and the sound wave appears in the bar.
     */
    fun setVoiceInputActive(active: Boolean) {
        if (voiceInputActive == active) return
        voiceInputActive = active
        if (hideKeyboardIsVoiceInput) {
            applyVoiceInputActiveState()
        }
    }

    private fun applyVoiceInputActiveState() {
        if (voiceInputActive) {
            hideKeyboardButton.startRecordingIndicator(
                theme.accentKeyBackgroundColor,
                theme.accentKeyTextColor
            )
            hideKeyboardButton.contentDescription = ctx.getString(R.string.stop_voice_input)
        } else {
            hideKeyboardButton.stopRecordingIndicator(
                theme.altKeyTextColor,
                theme.keyPressHighlightColor
            )
            hideKeyboardButton.contentDescription = ctx.getString(R.string.switch_to_voice_input)
        }
    }

    private fun clearAnimation() {
        animator.inAnimation = null
        animator.outAnimation = null
    }

    private fun setAnimation() {
        animator.inAnimation = inAnimation
        animator.outAnimation = outAnimation
    }

    private fun enableSlideTransition(inTarget: View, outTarget: View, inGravity: Int, outGravity: Int) {
        val slideIn = Slide(inGravity).apply { duration = 200L }
        val slideOut = Slide(outGravity).apply { duration = 200L }
        slideIn.addTarget(inTarget)
        slideOut.addTarget(outTarget)
        val set = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(slideIn)
            addTransition(slideOut)
        }
        TransitionManager.beginDelayedTransition(root, set)
    }

    fun updateState(state: State, fromUser: Boolean = false) {
        Timber.d("Switch idle ui to $state")
        if (
            !fromUser ||
            disableAnimation ||
            (state == State.InlineSuggestion || currentState == State.InlineSuggestion) ||
            (state == State.NumberRow || currentState == State.NumberRow)
        ) {
            clearAnimation()
        } else {
            setAnimation()
        }
        when (state) {
            State.Empty -> animator.displayedChild = 0
            State.Toolbar -> animator.displayedChild = 1
            State.Clipboard -> animator.displayedChild = 2
            State.NumberRow -> {}
            State.InlineSuggestion -> animator.displayedChild = 3
        }
        if (state == State.NumberRow) {
            numberRow.keyActionListener = commonKeyActionListener.listener
            numberRow.popupActionListener = popup.listener
            if (fromUser && !disableAnimation) {
                enableSlideTransition(numberRow, idleBody, Gravity.END, Gravity.START)
            }
            numberRow.visibility = View.VISIBLE
            idleBody.visibility = View.GONE
        } else if (currentState == State.NumberRow) {
            if (fromUser && !disableAnimation) {
                enableSlideTransition(idleBody, numberRow, Gravity.START, Gravity.END)
            }
            idleBody.visibility = View.VISIBLE
            numberRow.visibility = View.GONE
            numberRow.keyActionListener = null
            numberRow.popupActionListener = null
            popup.dismissAll()
        }
        currentState = state
        updateMenuButtonContentDescription()
        updateMenuButtonRotation(instant = !fromUser)
    }
}
