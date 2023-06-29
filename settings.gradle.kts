pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

include(":lib:fcitx5")
include(":codegen")
include(":app")
include(":lib:plugin-base")
include(":plugin:anthy")
rootProject.name = "fcitx5-android"
include(":lib:common")
include(":plugin:clipboard-filter")
