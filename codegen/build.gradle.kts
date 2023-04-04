plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp)
}

kotlin {
    jvmToolchain(11)
}
