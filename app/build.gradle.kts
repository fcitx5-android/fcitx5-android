@file:Suppress("UnstableApiUsage")

plugins {
    id("android-app-convention")
    id("native-app-convention")
    id("build-metadata")
    id("data-descriptor")
    id("fcitx-component")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fcitx.fcitx5.android"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                targets(
                    // jni
                    "native-lib",
                    // copy fcitx5 built-in addon libraries
                    "copy-fcitx5-modules",
                    // android specific modules
                    "androidfrontend",
                    "androidkeyboard",
                    // fcitx5-chinese-addons
                    "pinyin",
                    "scel2org5",
                    "table",
                    "chttrans",
                    "fullwidth",
                    "pinyinhelper",
                    "punctuation",
                    // fcitx5-lua
                    "luaaddonloader",
                    // fcitx5-unikey
                    "unikey"
                )
            }
        }
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("mipmap", "app_icon", "@mipmap/ic_launcher")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round")
            resValue("string", "app_name", "@string/app_name_release")
        }
        debug {
            resValue("mipmap", "app_icon", "@mipmap/ic_launcher_debug")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round_debug")
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}

aboutLibraries {
    configPath = "app/licenses"
}

fcitxComponent {
    installFcitx5Data = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":lib:fcitx5"))
    implementation(libs.autofill)
    implementation(libs.ini4j)
    ksp(project(":codegen"))
    implementation(libs.material)
    implementation(libs.arrow)
    implementation(libs.activity)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.imagecropper)
    implementation(libs.flexbox)
    implementation(libs.dependency)
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.timber)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.common)
    implementation(libs.coroutines)
    implementation(libs.preference)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.paging)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.splitties.bitflags)
    implementation(libs.splitties.dimensions)
    implementation(libs.splitties.resources)
    implementation(libs.splitties.views.dsl)
    implementation(libs.splitties.views.dsl.appcompat)
    implementation(libs.splitties.views.dsl.constraintlayout)
    implementation(libs.splitties.views.dsl.coordinatorlayout)
    implementation(libs.splitties.views.dsl.recyclerview)
    implementation(libs.splitties.views.recyclerview)
    implementation(libs.aboutlibraries)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.lifecycle.testing)
    androidTestImplementation(libs.lifecycle.testing)
    androidTestImplementation(libs.junit)
}
