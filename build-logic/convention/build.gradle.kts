plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `maven-publish`
    id("com.palantir.git-version") version "3.0.0"
    `java-gradle-plugin`
}

group = "org.fcitx.fcitx5.android.build_logic"

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
version = details.gitHash

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
