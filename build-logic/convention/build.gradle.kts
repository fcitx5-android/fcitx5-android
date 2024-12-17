import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `maven-publish`
    alias(libs.plugins.gitVersion)
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
    // A workaround to enable version catalog usage in the convention plugin,
    // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(LibrariesForLibs::class.java.protectionDomain.codeSource.location))
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
