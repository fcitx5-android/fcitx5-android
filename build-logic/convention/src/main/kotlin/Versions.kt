/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

import org.gradle.api.JavaVersion

object Versions {

    val java = JavaVersion.VERSION_11
    const val compileSdk = 35
    const val minSdk = 23
    const val targetSdk = 35

    const val defaultCMake = "3.31.6"
    const val defaultNDK = "28.0.13004108"
    const val defaultBuildTools = "35.0.1"

    // NOTE: increase this value to bump version code
    const val baseVersionCode = 9
    const val baseVersionName = "0.1.1"

    val supportedABIs = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    const val fallbackABI = "arm64-v8a"

    fun calculateVersionCode(abi: String = fallbackABI): Int {
        val abiId = when (abi) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> 0
        }
        return baseVersionCode * 10 + abiId
    }
}
