plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.plugin-app-convention")
    id("org.fcitx.fcitx5.android.native-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    id("org.fcitx.fcitx5.android.fcitx-component")
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.jyutping"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.jyutping"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    "jyutping",
                    "libime_jyutpingdict"
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

    packaging {
        jniLibs {
            excludes += setOf(
                "**/libc++_shared.so",
                "**/libFcitx5*",
                "**/libIMECore.so",
                "**/libpunctuation.so"
            )
        }
    }
}

fcitxComponent {
    modifyFiles = mapOf(
        "usr/share/fcitx5/addon/jyutping.conf" to {
            it.writeText(
                it.readText().replace(
                    "Library=libjyutping",
                    "Library=libIMEJyutping;libjyutping"
                )
            )
        }
    )
    installPrebuiltAssets = true
}

dependencies {
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:libime"))
    implementation(project(":lib:fcitx5-chinese-addons"))
    implementation(project(":lib:plugin-base"))
}
