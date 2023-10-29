/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data

import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.audioManager
import org.fcitx.fcitx5.android.utils.isSystemSettingEnabled
import org.fcitx.fcitx5.android.utils.vibrator

object InputFeedbacks {

    enum class InputFeedbackMode {
        Enabled, Disabled, FollowingSystem;

        companion object : ManagedPreference.StringLikeCodec<InputFeedbackMode> {
            override fun decode(raw: String) = InputFeedbackMode.valueOf(raw)
        }
    }

    private var systemSoundEffects = false
    private var systemHapticFeedback = false

    fun syncSystemPrefs() {
        systemSoundEffects = isSystemSettingEnabled(Settings.System.SOUND_EFFECTS_ENABLED)
        // it says "Replaced by using android.os.VibrationAttributes.USAGE_TOUCH"
        // but gives no clue about how to use it, and this one still works
        @Suppress("DEPRECATION")
        systemHapticFeedback = isSystemSettingEnabled(Settings.System.HAPTIC_FEEDBACK_ENABLED)
    }

    private val soundOnKeyPress by AppPrefs.getInstance().keyboard.soundOnKeyPress
    private val soundOnKeyPressVolume by AppPrefs.getInstance().keyboard.soundOnKeyPressVolume
    private val hapticOnKeyPress by AppPrefs.getInstance().keyboard.hapticOnKeyPress
    private val buttonPressVibrationMilliseconds by AppPrefs.getInstance().keyboard.buttonPressVibrationMilliseconds
    private val buttonLongPressVibrationMilliseconds by AppPrefs.getInstance().keyboard.buttonLongPressVibrationMilliseconds
    private val buttonPressVibrationAmplitude by AppPrefs.getInstance().keyboard.buttonPressVibrationAmplitude
    private val buttonLongPressVibrationAmplitude by AppPrefs.getInstance().keyboard.buttonLongPressVibrationAmplitude

    private val vibrator = appContext.vibrator

    private val hasAmplitudeControl =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && vibrator.hasAmplitudeControl()

    private val audioManager = appContext.audioManager

    fun hapticFeedback(view: View, longPress: Boolean = false) {
        when (hapticOnKeyPress) {
            InputFeedbackMode.Enabled -> {}
            InputFeedbackMode.Disabled -> return
            InputFeedbackMode.FollowingSystem -> if (!systemHapticFeedback) return
        }

        val duration: Long
        val amplitude: Int
        val hfc: Int
        if (longPress) {
            duration = buttonLongPressVibrationMilliseconds.toLong()
            amplitude = buttonLongPressVibrationAmplitude
            hfc = HapticFeedbackConstants.LONG_PRESS
        } else {
            duration = buttonPressVibrationMilliseconds.toLong()
            amplitude = buttonPressVibrationAmplitude
            hfc = HapticFeedbackConstants.KEYBOARD_TAP
        }
        val useVibrator = duration != 0L

        if (useVibrator) {
            // on Android 13, if system haptic feedback was disabled, `vibrator.vibrate()` won't work
            // but `view.performHapticFeedback()` with `FLAG_IGNORE_GLOBAL_SETTING` still works
            if (hasAmplitudeControl && amplitude != 0) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } else {
            // it says "Starting TIRAMISU only privileged apps can ignore user settings for touch feedback"
            // but we still seem to be able to use `FLAG_IGNORE_GLOBAL_SETTING`
            @Suppress("DEPRECATION")
            val flags =
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            view.performHapticFeedback(hfc, flags)
        }
    }

    enum class SoundEffect {
        Standard, SpaceBar, Delete, Return
    }

    fun soundEffect(effect: SoundEffect) {
        when (soundOnKeyPress) {
            InputFeedbackMode.Enabled -> {}
            InputFeedbackMode.Disabled -> return
            InputFeedbackMode.FollowingSystem -> if (!systemSoundEffects) return
        }
        val fx = when (effect) {
            SoundEffect.Standard -> AudioManager.FX_KEYPRESS_STANDARD
            SoundEffect.SpaceBar -> AudioManager.FX_KEYPRESS_SPACEBAR
            SoundEffect.Delete -> AudioManager.FX_KEYPRESS_DELETE
            SoundEffect.Return -> AudioManager.FX_KEYPRESS_RETURN
        }
        val volume = soundOnKeyPressVolume
        if (volume == 0) {
            audioManager.playSoundEffect(fx, -1f)
        } else {
            audioManager.playSoundEffect(fx, volume / 100f)
        }
    }

}