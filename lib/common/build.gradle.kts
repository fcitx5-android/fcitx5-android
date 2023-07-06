plugins {
    id("org.fcitx.fcitx5.android.lib-convention")
    `maven-publish`
    id("com.palantir.git-version") version "3.0.0"
}

android {
    namespace = "org.fcitx.fcitx5.android.lib.common"

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        aidl = true
    }
    publishing {
        singleVariant("release")
    }
}

val gitVersion: groovy.lang.Closure<String> by extra
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
