import com.android.build.gradle.tasks.MergeSourceSetFolders
import kotlinx.serialization.json.Json

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
        doLast {
            val inFile = file("ClearURLsRules/data.min.json")
            val outFile = outputDir.asFile.get().resolve("data.min.json")
            // minify json
            outFile.writeText(Json.parseToJsonElement(inFile.readText()).toString())
        }
    }
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
}
