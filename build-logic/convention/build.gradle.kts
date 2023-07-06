plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `maven-publish`
    id("com.palantir.git-version") version "3.0.0"
    `java-gradle-plugin`
}

group = "org.fcitx.fcitx5.android.build_logic"

val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

java {
    withSourcesJar()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.aboutlibraries.plugin)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("androidAppConvention") {
            id = "org.fcitx.fcitx5.android.app-convention"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("androidLibConvention") {
            id = "org.fcitx.fcitx5.android.lib-convention"
            implementationClass = "AndroidLibConventionPlugin"
        }
        register("androidPluginAppConvention") {
            id = "org.fcitx.fcitx5.android.plugin-app-convention"
            implementationClass = "AndroidPluginAppConventionPlugin"
        }
        register("buildMetadata") {
            id = "org.fcitx.fcitx5.android.build-metadata"
            implementationClass = "BuildMetadataPlugin"
        }
        register("cmakeDir") {
            id = "org.fcitx.fcitx5.android.cmake-dir"
            implementationClass = "CMakeDirPlugin"
        }
        register("dataDescriptor") {
            id = "org.fcitx.fcitx5.android.data-descriptor"
            implementationClass = "DataDescriptorPlugin"
        }
        register("fcitxComponent") {
            id = "org.fcitx.fcitx5.android.fcitx-component"
            implementationClass = "FcitxComponentPlugin"
        }
        register("fcitxHeaders") {
            id = "org.fcitx.fcitx5.android.fcitx-headers"
            implementationClass = "FcitxHeadersPlugin"
        }
        register("nativeAppConvention") {
            id = "org.fcitx.fcitx5.android.native-app-convention"
            implementationClass = "NativeAppConventionPlugin"
        }
        register("nativeLibConvention") {
            id = "org.fcitx.fcitx5.android.native-lib-convention"
            implementationClass = "NativeLibConventionPlugin"
        }
        register("androidSdkPath") {
            id = "org.fcitx.fcitx5.android.android-sdk-path"
            implementationClass = "AndroidSdkPathPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/fcitx5-android/fcitx5-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
