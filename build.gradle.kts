plugins {
    id("com.android.application") version "7.3.1" apply false
    kotlin("android") version "1.8.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
