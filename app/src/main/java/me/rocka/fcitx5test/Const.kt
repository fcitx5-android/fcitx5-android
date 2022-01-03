package me.rocka.fcitx5test

import java.text.SimpleDateFormat
import java.util.*

object Const {
    private val buildTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)
        .format(Date(BuildConfig.BUILD_TIME))
    val versionInfo =
        """
        Build Git Hash: ${BuildConfig.BUILD_GIT_HASH}
        Build Date: $buildTime
        """.trimIndent()
    const val assetDescriptorName = BuildConfig.ASSETS_DESCRIPTOR_NAME
}