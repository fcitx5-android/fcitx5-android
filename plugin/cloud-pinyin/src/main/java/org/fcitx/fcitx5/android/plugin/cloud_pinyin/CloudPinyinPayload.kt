/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.plugin.cloud_pinyin

import kotlinx.serialization.Serializable

@Serializable
data class CloudPinyinPayload(
    val backend: Int,
    val pinyin: String,
    val proxy: String? = null
)