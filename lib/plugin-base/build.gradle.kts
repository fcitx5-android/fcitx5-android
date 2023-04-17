plugins {
    id("android-lib-convention")
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.plugin_base"
}

dependencies {
    implementation(libs.aboutlibraries)
}
