@file:Suppress("UnstableApiUsage")

plugins {
    id("org.fcitx.fcitx5.android.lib-convention")
    id("org.fcitx.fcitx5.android.native-lib-convention")
    id("org.fcitx.fcitx5.android.fcitx-headers")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.fcitx5"

    defaultConfig {
        externalNativeBuild {
            cmake {
                targets(
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
}
