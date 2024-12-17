plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.plugin-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.clipboard_filter"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.clipboard_filter"
    }

    sourceSets {
        getByName("main") {
            // TODO: only include data.min.json
            assets.setSrcDirs(listOf("src/main/assets", "ClearURLsRules"))
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
}

aboutLibraries {
    configPath = "plugin/clipboard-filter/licenses"
}

generateDataDescriptor{
    excludes.add("data.min.json")
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
}
