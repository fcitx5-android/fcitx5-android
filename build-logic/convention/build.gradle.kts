import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = "org.fcitx.fcitx5.android.buildlogic"

kotlin {
    jvmToolchain(11)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("androidAppConvention") {
            id = "android-app-convention"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("androidLibConvention") {
            id = "android-lib-convention"
            implementationClass = "AndroidLibConventionPlugin"
        }
        register("buildMetadata") {
            id = "build-metadata"
            implementationClass = "BuildMetadataPlugin"
        }
        register("cmakeDir") {
            id = "cmake-dir"
            implementationClass = "CMakeDirPlugin"
        }
        register("dataDescriptor") {
            id = "data-descriptor"
            implementationClass = "DataDescriptorPlugin"
        }
        register("fcitxComponent") {
            id = "fcitx-component"
            implementationClass = "FcitxComponentPlugin"
        }
        register("fcitxHeaders") {
            id = "fcitx-headers"
            implementationClass = "FcitxHeadersPlugin"
        }
        register("nativeAppConvention") {
            id = "native-app-convention"
            implementationClass = "NativeAppConventionPlugin"
        }
        register("nativeLibConvention") {
            id = "native-lib-convention"
            implementationClass = "NativeLibConventionPlugin"
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}
