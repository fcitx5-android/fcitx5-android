import com.android.build.gradle.tasks.MergeSourceSetFolders

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

    buildTypes {
        release {
            resValue("string", "app_name", "@string/app_name_release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }
}

// copy ClearURLsRules/data.min.json to apk assets
tasks.withType<MergeSourceSetFolders>().all {
    // mergeDebugAssets or mergeReleaseAssets
    if (name.endsWith("Assets")) {
        val outDir = outputDir.asFile.get()
        doLast {
            file("ClearURLsRules/data.min.json").copyTo(outDir.resolve("data.min.json"))
        }
    }
}

aboutLibraries {
    configPath = "plugin/clipboard-filter/licenses"
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
}
