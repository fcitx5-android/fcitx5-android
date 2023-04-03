import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = "org.fcitx.fcitx5.android.buildlogic"

kotlin {
    jvmToolchain(11)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("dataDescriptor") {
            id = "data-descriptor"
            implementationClass = "DataDescriptorPlugin"
        }
        register("fcitxComponent") {
            id = "fcitx-component"
            implementationClass = "FcitxComponentPlugin"
        }
        register("buildMetadata") {
            id = "build-metadata"
            implementationClass = "BuildMetadataPlugin"
        }
        register("androidConvention") {
            id = "android-convention"
            implementationClass = "AndroidConventionPlugin"
        }
        register("nativeConvention") {
            id = "native-convention"
            implementationClass = "NativeConventionPlugin"
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}
