@file:Suppress("UnstableApiUsage")

plugins {
    id("android-app-convention")
    id("android-plugin-app-convention")
    id("build-metadata")
    id("data-descriptor")
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

configure<DataDescriptorPluginExtension> {
    excludes.set(listOf("data.minify.json"))
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
}
