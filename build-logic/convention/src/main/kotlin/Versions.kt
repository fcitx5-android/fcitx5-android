/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import org.gradle.api.JavaVersion
import org.gradle.api.Project

object Versions {

    val java = JavaVersion.VERSION_1_8
    const val compileSdk = 34
    const val minSdk = 23
    const val targetSdk = 34

    val defaultABI = "arm64-v8a"
    val supportedABIs = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

    private const val defaultCMake = "3.22.1"
    private const val defaultNDK = "25.2.9519653"
    private const val defaultBuildTools = "34.0.0"

    // NOTE: increase this value to bump version code
    private const val baseVersionCode = 7

    fun calculateVersionCode(abi: String? = defaultABI): Int {
        val abiId = when (abi ?: defaultABI) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> 0
        }
        return baseVersionCode * 10 + abiId
    }

    val Project.cmakeVersion
        get() = ep("CMAKE_VERSION", "cmakeVersion") { defaultCMake }

    val Project.ndkVersion
        get() = ep("NDK_VERSION", "ndkVersion") { defaultNDK }

    val Project.buildTools
        get() = ep("BUILD_TOOLS_VERSION", "buildTools") { defaultBuildTools }
}