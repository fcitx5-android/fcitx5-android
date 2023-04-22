plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = "org.fcitx.fcitx5.android.build_logic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.aboutlibrariesPlugin)
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
        register("androidPluginAppConvention") {
            id = "android-plugin-app-convention"
            implementationClass = "AndroidPluginAppConventionPlugin"
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
