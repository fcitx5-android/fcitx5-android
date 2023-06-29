plugins {
    id("android-lib-convention")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.common"

    buildTypes {
        release {
            // disable obfuscation for plugins depend on plugin-base
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        aidl = true
    }
}

