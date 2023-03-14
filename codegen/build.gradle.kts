plugins {
    id("java-library")
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
}