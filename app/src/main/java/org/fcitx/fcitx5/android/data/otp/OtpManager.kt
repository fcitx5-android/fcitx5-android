/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.otp

import org.fcitx.fcitx5.android.utils.WeakHashSet

object OtpManager {
    fun interface OnOtpReceivedListener {
        fun onOtpReceived(otp: String)
    }

    private val listeners = WeakHashSet<OnOtpReceivedListener>()

    fun addOnOtpReceivedListener(listener: OnOtpReceivedListener) {
        listeners.add(listener)
    }

    fun removeOnOtpReceivedListener(listener: OnOtpReceivedListener) {
        listeners.remove(listener)
    }

    fun updateOtp(otp: String) {
        listeners.forEach { it.onOtpReceived(otp) }
    }
}
