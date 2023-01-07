package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.BuildConfig

object Const {
    const val buildTime = BuildConfig.BUILD_TIME
    const val buildGitHash = BuildConfig.BUILD_GIT_HASH
    const val versionName = "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}"
    const val dataDescriptorName = BuildConfig.DATA_DESCRIPTOR_NAME
    const val githubRepo = "https://github.com/fcitx5-android/fcitx5-android"
    const val lgpl = "LGPL-2.1-or-later"
    const val lgplLicenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1"
    const val privacyPolicyUrl = "https://fcitx5-android.github.io/privacy/"
    const val faqUrl = "https://fcitx5-android.github.io/faq/"
}