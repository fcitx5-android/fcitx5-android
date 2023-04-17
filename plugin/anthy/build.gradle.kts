@file:Suppress("UnstableApiUsage")

plugins {
    id("android-app-convention")
    id("android-plugin-app-convention")
    id("native-app-convention")
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
        release {
            resValue("string", "app_name", "@string/app_name_release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
        }
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

aboutLibraries {
    configPath = "plugin/anthy/licenses"
}

dependencies {
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:plugin-base"))
}
