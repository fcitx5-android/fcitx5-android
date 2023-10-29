/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.view.KeyEvent
import splitties.bitflags.hasFlag

infix fun UInt.or(other: KeyState): UInt = this or other.state

/**
 * translated from
 * [fcitx-utils/keysym.h](https://github.com/fcitx/fcitx5/blob/0346e58/src/lib/fcitx-utils/keysym.h)
 */
@Suppress("Unused", "EnumEntryName")
enum class KeyState(val state: UInt) {
    NoState(0u),
    Shift(1u shl 0),
    CapsLock(1u shl 1),
    Ctrl(1u shl 2),
    Alt(1u shl 3),
    Mod1u(Alt),
    Alt_Shift(Alt or Shift),
    Ctrl_Shift(Ctrl or Shift),
    Ctrl_Alt(Ctrl or Alt),
    Ctrl_Alt_Shift(Ctrl or Alt or Shift),
    NumLock(1u shl 4),
    Mod2(NumLock),
    Hyper(1u shl 5),
    Mod3(Hyper),
    Super(1u shl 6),
    Mod4(Super),
    Mod5(1u shl 7),
    MousePressed(1u shl 8),
    HandledMask(1u shl 24),
    IgnoredMask(1u shl 25),
    Super2(1u shl 26), // Gtk virtual Super
    Hyper2(1u shl 27), // Gtk virtual Hyper
    Meta(1u shl 28),
    Virtual(1u shl 29),

    /**
     * Whether a Key Press is from key repetition.
     */
    Repeat(1u shl 31),
    UsedMask(0x5c001fffu),
    SimpleMask(Ctrl_Alt_Shift or Super or Super2 or Hyper or Meta);

    constructor(other: KeyState) : this(other.state)

    infix fun or(other: KeyState): UInt = state or other.state

    infix fun or(other: UInt): UInt = state or other
}

operator fun UInt.plus(other: KeyState) = or(other.state)
operator fun UInt.minus(other: KeyState) = and(other.state.inv())

@JvmInline
value class KeyStates(val states: UInt) {

    constructor(vararg states: KeyState) : this(mergeStates(states))

    fun has(state: KeyState) = states.hasFlag(state.state)

    val alt get() = has(KeyState.Alt)
    val ctrl get() = has(KeyState.Ctrl)
    val shift get() = has(KeyState.Shift)
    val meta get() = has(KeyState.Meta)
    val numLock get() = has(KeyState.NumLock)
    val capsLock get() = has(KeyState.CapsLock)

    /**
     * **used in fcitx5-android only**
     */
    val virtual get() = has(KeyState.Virtual)

    val metaState: Int get() {
        var metaState = 0
        if (alt) metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (ctrl) metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (meta) metaState = metaState or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (numLock) metaState = metaState or KeyEvent.META_NUM_LOCK_ON
        if (capsLock) metaState = metaState or KeyEvent.META_CAPS_LOCK_ON
        return metaState
    }

    fun toInt() = states.toInt()

    companion object {
        val Empty = KeyStates(0u)

        fun of(v: Int) = KeyStates(v.toUInt())

        fun fromKeyEvent(event: KeyEvent): KeyStates {
            var states = KeyState.NoState.state
            event.apply {
                if (isAltPressed) states += KeyState.Alt
                if (isCtrlPressed) states += KeyState.Ctrl
                if (isShiftPressed) states += KeyState.Shift
                if (isCapsLockOn) states += KeyState.CapsLock
                if (isNumLockOn) states += KeyState.NumLock
                if (isMetaPressed) states += KeyState.Meta
//                if (isFunctionPressed);
//                if (isSymPressed);
//                if (isSystem);
            }
            return KeyStates(states)
        }

        fun mergeStates(arr: Array<out KeyState>): UInt =
            arr.fold(KeyState.NoState.state) { acc, it -> acc or it.state }
    }
}
