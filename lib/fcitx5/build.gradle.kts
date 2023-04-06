@file:Suppress("UnstableApiUsage")

import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("lib-native-convention")
    id("fcitx-headers")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.fcitx5"

    defaultConfig {
        externalNativeBuild {
            cmake {
                targets += setOf(
                    // dummy "cmake" target
                    "cmake",
                    // fcitx5
                    "Fcitx5Core",
                    "Fcitx5Config",
                    "Fcitx5Utils",
                    // fcitx5 modules
                    "clipboard",
                    "imselector",
                    "notifications",
                    "quickphrase",
                    "spell",
                    "unicode"
                )
            }
        }
    }

    prefab {
        create("cmake") {
            headerOnly = true
            headers = "src/main/cpp/cmake"
        }
        val headersPrefix = "build/headers/usr/include/Fcitx5"
        create("Fcitx5Core") {
            libraryName = "libFcitx5Core"
            headers = "$headersPrefix/Core"
        }
        create("Fcitx5Config") {
            libraryName = "libFcitx5Config"
            headers = "$headersPrefix/Config"
        }
        create("Fcitx5Utils") {
            libraryName = "libFcitx5Utils"
            headers = "$headersPrefix/Utils"
        }
        val moduleHeadersPrefix = "$headersPrefix/Module/fcitx-module"
        create("clipboard") {
            libraryName = "libclipboard"
            headers = "$moduleHeadersPrefix/clipboard"
        }
        create("imselector") {
            libraryName = "libimselector"
            // module imselector has no public headers
        }
        create("notifications") {
            // module notifications is not built since dbus is disabled
            headerOnly = true
            headers = "$moduleHeadersPrefix/notifications"
        }
        create("quickphrase") {
            libraryName = "libquickphrase"
            headers = "$moduleHeadersPrefix/quickphrase"
        }
        create("spell") {
            libraryName = "libspell"
            headers = "$moduleHeadersPrefix/spell"
        }
        create("unicode") {
            libraryName = "libunicode"
            headers = "$moduleHeadersPrefix/unicode"
        }
    }
    libraryVariants.all {
        // The output of PrefabConfigurePackageTask is up-to-date even after running clean.
        // This is probably a bug of AGP. To work around, we need always rerun this task.
        tasks.named("prefab${name.capitalized()}ConfigurePackage").configure {
            doNotTrackState("The up-to-date checking of PrefabConfigurePackageTask is incorrect")
        }
    }
}
