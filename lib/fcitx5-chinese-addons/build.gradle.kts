@file:Suppress("UnstableApiUsage")

plugins {
    id("org.fcitx.fcitx5.android.lib-convention")
    id("org.fcitx.fcitx5.android.native-lib-convention")
    id("org.fcitx.fcitx5.android.fcitx-headers")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.fcitx5_chinese_addons"

    defaultConfig {
        externalNativeBuild {
            cmake {
                targets(
                    // dummy "cmake" target
                    "cmake",
                    // fcitx5-chinese-addons
                    "pinyin",
                    "scel2org5",
                    "table",
                    "chttrans",
                    "fullwidth",
                    "pinyinhelper",
                    "punctuation",
                )
            }
        }
    }

    prefab {
        create("cmake") {
            headerOnly = true
            headers = "src/main/cpp/cmake"
        }
        create("pinyin") {
            libraryName = "libpinyin"
            // no headers
        }
        create("table") {
            libraryName = "libtable"
            // no headers
        }
        create("scel2org5") {
            libraryName = "libscel2org5"
            // no headers
        }
        val moduleHeadersPrefix = "build/headers/usr/include/Fcitx5/Module/fcitx-module"
        create("chttrans") {
            libraryName = "libchttrans"
            // no headers
        }
        create("fullwidth") {
            libraryName = "libfullwidth"
            // no headers
        }
        create("pinyinhelper") {
            libraryName = "libpinyinhelper"
            headers = "$moduleHeadersPrefix/pinyinhelper"
        }
        create("punctuation") {
            libraryName = "libpunctuation"
            headers = "$moduleHeadersPrefix/punctuation"
        }
    }
}

dependencies {
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:libime"))
    implementation(project(":lib:fcitx5-lua"))
}
