/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
@file:Suppress("UnstableApiUsage")

plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.plugin-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.clipboard_filter"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.clipboard_filter"
    }

    buildTypes {
        release {
            resValue("string", "app_name", "@string/app_name_release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }
}

generateDataDescriptor{
    excludes.add("data.minify.json")
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
}
