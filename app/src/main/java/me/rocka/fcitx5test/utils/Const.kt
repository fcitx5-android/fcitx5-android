package me.rocka.fcitx5test.utils

import me.rocka.fcitx5test.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

object Const {
    val buildTime: String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.ZZZZ",
        Locale.ROOT
    ).format(Date(BuildConfig.BUILD_TIME))
    const val buildGitHash = BuildConfig.BUILD_GIT_HASH
    const val versionName = BuildConfig.VERSION_NAME
    const val dataDescriptorName = BuildConfig.DATA_DESCRIPTOR_NAME
    const val githubRepo = "https://github.com/rocka/fcitx5-android-poc"
    const val lgpl = "LGPL-2.1-or-later"
    const val lgplLicenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1"
}