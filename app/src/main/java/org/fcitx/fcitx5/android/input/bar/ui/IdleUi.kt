/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Space
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
        rotation = menuButtonRotation
    }

    val hideKeyboardButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_drop_down_24, theme)

    val emptyBar = Space(ctx)

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

    override val root = constraintLayout {
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
        add(numberRow, lParams(matchParent, matchParent))
    }

    fun privateMode(activate: Boolean = true) {
        if (activate == inPrivate) return
        inPrivate = activate
        updateMenuButtonIcon()
        updateMenuButtonRotation(instant = true)
    }

    private fun updateMenuButtonIcon() {
        menuButton.image.imageResource =
            if (inPrivate) R.drawable.ic_view_private
            else R.drawable.ic_baseline_expand_more_24
    }

    private fun updateMenuButtonRotation(instant: Boolean = false) {
        val targetRotation = menuButtonRotation
        menuButton.apply {
            if (targetRotation == rotation) return
            animate().cancel()
            if (!instant && !disableAnimation) {
                animate().setDuration(200L).rotation(targetRotation)
            } else {
                rotation = targetRotation
            }
        }
    }

    fun setHideKeyboardIsVoiceInput(isVoiceInput: Boolean, callback: View.OnClickListener) {
        hideKeyboardButton.setIcon(
            if (isVoiceInput) R.drawable.ic_baseline_keyboard_voice_24
            else R.drawable.ic_baseline_arrow_drop_down_24
        )
        hideKeyboardButton.setOnClickListener(callback)
    }

    private fun clearAnimation() {
        animator.inAnimation = null
        animator.outAnimation = null
    }

    private fun setAnimation() {
        animator.inAnimation = inAnimation
        animator.outAnimation = outAnimation
    }

    fun updateState(state: State, fromUser: Boolean = false) {
        Timber.d("Switch idle ui to $state")
        if (
            !fromUser ||
            disableAnimation ||
            (state == State.InlineSuggestion || currentState == State.InlineSuggestion)
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
            menuButton.visibility = View.GONE
            hideKeyboardButton.visibility = View.GONE
            animator.visibility = View.GONE
            numberRow.visibility = View.VISIBLE
            numberRow.keyActionListener = commonKeyActionListener.listener
            numberRow.popupActionListener = popup.listener
        } else {
            menuButton.visibility = View.VISIBLE
            hideKeyboardButton.visibility = View.VISIBLE
            animator.visibility = View.VISIBLE
            numberRow.visibility = View.GONE
            numberRow.keyActionListener = null
            numberRow.popupActionListener = null
            popup.dismissAll()
        }
        currentState = state
        updateMenuButtonRotation(instant = !fromUser)
    }
}
