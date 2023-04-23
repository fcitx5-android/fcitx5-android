plugins {
    id("android-lib-convention")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.plugin_base"

    buildTypes {
        release {
            // disable obfuscation for plugins depend on plugin-base
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.aboutlibraries.core)
}
