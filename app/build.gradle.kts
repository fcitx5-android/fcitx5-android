import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android")
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    ndkVersion = "23.1.7779620"

    defaultConfig {
        applicationId = "me.rocka.fcitx5test"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "0.0.1"
        buildConfigField("String", "BUILD_GIT_HASH", "\"${gitHashShort()}\"")
        buildConfigField("long", "BUILD_TIME", System.currentTimeMillis().toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            version = "3.18.1"
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

fun gitHashShort(): String  = ByteArrayOutputStream().let {
    project.exec {
        commandLine = "git describe --tags --always --dirty".split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.lifecycle", "lifecycle-runtime-ktx", "2.3.1")
    implementation("androidx.lifecycle:lifecycle-service:2.4.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.4.2")
    implementation("androidx.preference:preference-ktx:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.4.0")
    androidTestImplementation("junit:junit:4.13.2")
}