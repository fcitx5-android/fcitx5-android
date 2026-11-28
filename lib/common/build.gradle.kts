plugins {
    id("org.fcitx.fcitx5.android.lib-convention")
    id("org.fcitx.fcitx5.android.native-lib-convention")
    `maven-publish`
    alias(libs.plugins.gitVersion)
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.common"

    buildFeatures {
        aidl = true
    }
    publishing {
        singleVariant("release")
    }

    defaultConfig {
        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    "android-ipc-bridge"
                )
            }
        }
    }

    prefab {
        create("android-ipc-bridge") {
            headerOnly = true
            headers = "src/main/cpp/headers"
        }
    }
}

val gitVersion = extra["gitVersion"] as groovy.lang.Closure<*>
version = gitVersion()

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
    publications {
        register<MavenPublication>("release") {
            groupId = "org.fcitx.fcitx5.android.lib"
            artifactId = "common"
            pom {
                licenses {
                    name.set("LGPL-2.1")
                    url.set("https://spdx.org/licenses/LGPL-2.1.html")
                }
            }
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
