plugins {
    id("org.fcitx.fcitx5.android.lib-convention")
    id("org.fcitx.fcitx5.android.native-lib-convention")
    id("org.fcitx.fcitx5.android.fcitx-headers")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.fcitx5_lua"

    defaultConfig {
        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    // dummy "cmake" target
                    "cmake",
                    // fcitx5-lua
                    "luaaddonloader"
                )
            }
        }
    }

    prefab {
        create("cmake") {
            headers = "src/main/cpp/cmake"
            headerOnly = true
        }
        val moduleHeadersPrefix = "build/headers/usr/include/Fcitx5/Module/fcitx-module"
        create("luaaddonloader") {
            libraryName = "libluaaddonloader"
            headers = "$moduleHeadersPrefix/luaaddonloader"
        }
    }
}

dependencies {
    implementation(project(":lib:fcitx5"))
}
