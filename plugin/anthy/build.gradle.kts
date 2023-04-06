@file:Suppress("UnstableApiUsage")

plugins {
    id("android-convention")
    id("app-native-convention")
    id("build-metadata")
    id("data-descriptor")
    id("fcitx-component")
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.anthy"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.anthy"

        externalNativeBuild {
            cmake {
                targets(
                    "anthy"
                )
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        prefab = true
    }

    packagingOptions {
        jniLibs {
            excludes += setOf(
                "**/libc++_shared.so",
                "**/libFcitx5*",
                "**/libclipboard.so",
                "**/libimselector.so",
                "**/libquickphase.so",
                "**/libspell.so",
                "**/libtest*",
                "**/libunicode.so",
            )
        }
    }
}

dependencies {
    implementation(project(":lib:fcitx5"))
}
