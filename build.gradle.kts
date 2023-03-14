plugins {
    id("com.android.application") version "7.4.2" apply false
    kotlin("android") version "1.8.10" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.6.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
