plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.plugin-app-convention")
    id("org.fcitx.fcitx5.android.native-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    id("org.fcitx.fcitx5.android.fcitx-component")
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.rime"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.rime"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    "rime"
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
                "**/libFcitx5*"
            )
        }
    }
}

val copyRimeSharedData = tasks.register<Copy>("copyRimeSharedData") {
    from(listOf(
        "default.yaml",
        "rime-essay/essay.txt",
        "rime-prelude/key_bindings.yaml",
        "rime-prelude/punctuation.yaml",
        "rime-prelude/symbols.yaml",
        "rime-luna-pinyin/luna_pinyin_fluency.schema.yaml",
        "rime-luna-pinyin/luna_pinyin_simp.schema.yaml",
        "rime-luna-pinyin/luna_pinyin_tw.schema.yaml",
        "rime-luna-pinyin/luna_pinyin.dict.yaml",
        "rime-luna-pinyin/luna_pinyin.schema.yaml",
        "rime-luna-pinyin/luna_quanpin.schema.yaml",
        "rime-luna-pinyin/pinyin.yaml",
        "rime-stroke/stroke.dict.yaml",
        "rime-stroke/stroke.schema.yaml",
    ).map { "src/main/cpp/$it" })
    into(layout.projectDirectory.dir("src/main/assets/usr/share/rime-data/"))
}

tasks.withType<DataDescriptorPlugin.DataDescriptorTask>().all {
    dependsOn(copyRimeSharedData)
}

generateDataDescriptor {
    symlinks.put("usr/share/rime-data/opencc", "usr/share/opencc")
}

aboutLibraries {
    configPath = "plugin/rime/licenses"
}

dependencies {
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:plugin-base"))
}
