/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.BuildConfig

object Const {
    const val buildTime = BuildConfig.BUILD_TIME
    const val buildGitHash = BuildConfig.BUILD_GIT_HASH
    const val versionName = "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}"
    const val dataDescriptorName = BuildConfig.DATA_DESCRIPTOR_NAME
    const val githubRepo = "https://github.com/fcitx5-android/fcitx5-android"
    const val licenseSpdxId = "LGPL-2.1-or-later"
    const val licenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1"
    const val privacyPolicyUrl = "https://fcitx5-android.github.io/privacy/"
    const val faqUrl = "https://fcitx5-android.github.io/faq/"
    const val buildType = BuildConfig.BUILD_TYPE
}