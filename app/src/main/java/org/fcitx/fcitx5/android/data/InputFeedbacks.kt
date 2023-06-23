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
import org.fcitx.fcitx5.android.utils.getSystemSetting
import org.fcitx.fcitx5.android.utils.vibrator

object InputFeedbacks {

    enum class InputFeedbackMode {
        Enabled, Disabled, FollowingSystem;

        companion object : ManagedPreference.StringLikeCodec<InputFeedbackMode> {
            override fun decode(raw: String) = InputFeedbackMode.valueOf(raw)
        }
    }

    private var systemSoundEffectsEnabled = false

    fun syncSystemPrefs() {
        systemSoundEffectsEnabled = getSystemSetting(Settings.System.SOUND_EFFECTS_ENABLED)
    }

    private val soundOnKeyPress by AppPrefs.getInstance().keyboard.soundOnKeyPress
    private val soundOnKeyPressVolume by AppPrefs.getInstance().keyboard.soundOnKeyPressVolume
    private val hapticOnKeyPress by AppPrefs.getInstance().keyboard.hapticOnKeyPress
    private val buttonPressVibrationMilliseconds by AppPrefs.getInstance().keyboard.buttonPressVibrationMilliseconds
    private val buttonLongPressVibrationMilliseconds by AppPrefs.getInstance().keyboard.buttonLongPressVibrationMilliseconds
    private val buttonPressVibrationAmplitude by AppPrefs.getInstance().keyboard.buttonPressVibrationAmplitude
    private val buttonLongPressVibrationAmplitude by AppPrefs.getInstance().keyboard.buttonLongPressVibrationAmplitude

    private val audioManager = appContext.audioManager

    fun hapticFeedback(view: View, longPress: Boolean = false) {
        if (hapticOnKeyPress == InputFeedbackMode.Disabled)
            return
        val vibrator = view.vibrator
        val (duration, amplitude) =
            if (longPress) buttonLongPressVibrationMilliseconds to buttonPressVibrationMilliseconds
            else buttonLongPressVibrationAmplitude to buttonPressVibrationAmplitude

        val useVibrator = when (hapticOnKeyPress) {
            // TODO: support always enable
            InputFeedbackMode.Enabled,
            InputFeedbackMode.FollowingSystem -> duration != 0
            InputFeedbackMode.Disabled -> return
        }

        if (useVibrator) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp =
                    amplitude.takeIf { vibrator.hasAmplitudeControl() && it != 0 }
                        ?: VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(duration.toLong(), amp))
            } else {
                @Suppress("DEPRECATION")
                view.vibrator.vibrate(duration.toLong())
            }
        } else {
            view.performHapticFeedback(
                if (longPress) HapticFeedbackConstants.LONG_PRESS
                else HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    enum class SoundEffect {
        Standard, SpaceBar, Delete, Return
    }

    fun soundEffect(effect: SoundEffect) {
        when (soundOnKeyPress) {
            InputFeedbackMode.Enabled -> {}
            InputFeedbackMode.Disabled -> return
            InputFeedbackMode.FollowingSystem -> {
                if (!systemSoundEffectsEnabled) return
            }
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