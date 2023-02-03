@file:Suppress("UnstableApiUsage")

import android.databinding.tool.ext.capitalizeUS
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.common.hash.Hashing
import com.google.common.io.Files
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

fun exec(cmd: String): String = ByteArrayOutputStream().use {
    project.exec {
        commandLine = cmd.split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.8.0-1.0.8"
    id("com.cookpad.android.plugin.license-tools") version "1.2.8"
    kotlin("plugin.serialization") version "1.8.0"
    kotlin("plugin.parcelize")
}

val dataDescriptorName = "descriptor.json"

// NOTE: increase this value to bump version code
val baseVersionCode = 1

fun calculateVersionCode(abi: String): Int {
    val abiId = when (abi) {
        "armeabi-v7a" -> 1
        "arm64-v8a" -> 2
        "x86" -> 3
        "x86_64" -> 4
        else -> 0
    }
    return baseVersionCode * 10 + abiId
}

fun envOrDefault(env: String, default: () -> String): String {
    val v = System.getenv(env)
    return if (v.isNullOrBlank()) default() else v
}

fun propertyOrDefault(prop: String, default: () -> String): String {
    return try {
        project.property(prop)!!.toString()
    } catch (e: Exception) {
        default()
    }
}

val buildABI = envOrDefault("BUILD_ABI") {
    propertyOrDefault("buildABI") {
        "arm64-v8a"
    }
}

val buildVersionName = envOrDefault("BUILD_VERSION_NAME") {
    propertyOrDefault("buildVersionName") {
        exec("git describe --tags --long --always")
    }
}

val buildCommitHash = envOrDefault("BUILD_COMMIT_HASH") {
    propertyOrDefault("buildCommitHash") {
        exec("git rev-parse HEAD")
    }
}

val buildTimestamp = envOrDefault("BUILD_TIMESTAMP") {
    propertyOrDefault("buildTimestamp") {
        System.currentTimeMillis().toString()
    }
}

android {
    namespace = "org.fcitx.fcitx5.android"
    compileSdk = 33
    buildToolsVersion = "33.0.0"
    ndkVersion = System.getenv("NDK_VERSION") ?: "25.0.8775105"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android"
        minSdk = 23
        targetSdk = 33
        versionCode = calculateVersionCode(buildABI)
        versionName = buildVersionName
        setProperty("archivesBaseName", "$applicationId-$buildVersionName")
        buildConfigField("String", "BUILD_GIT_HASH", "\"${buildCommitHash}\"")
        buildConfigField("long", "BUILD_TIME", buildTimestamp)
        buildConfigField("String", "DATA_DESCRIPTOR_NAME", "\"${dataDescriptorName}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("mipmap", "app_icon", "@mipmap/ic_launcher")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round")
            resValue("string", "app_name", "@string/app_name_release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"

            resValue("mipmap", "app_icon", "@mipmap/ic_launcher_debug")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round_debug")
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(buildABI)
            isUniversalApk = false
        }
    }

    externalNativeBuild {
        cmake {
            version = "3.22.1"
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

val generateBuildMetadata by tasks.register("generateBuildMetadata") {
    doLast {
        val outputDir = file("build/outputs/apk")
        outputDir.mkdirs()
        val metadataFile = outputDir.resolve("build-metadata.json")
        metadataFile.writeText(
            JsonOutput.prettyPrint(
                JsonOutput.toJson(
                    mapOf(
                        "versionName" to buildVersionName,
                        "commitHash" to buildCommitHash,
                        "timestamp" to buildTimestamp
                    )
                )
            )
        )
    }

    dependsOn(tasks.find { it.name == "compileDebugKotlin" || it.name == "compileReleaseKotlin" })
}

// This task should have depended on buildCMakeABITask
val installFcitxComponent by tasks.register("installFcitxComponent")

val generateDataDescriptor by tasks.register<DataDescriptorTask>("generateDataDescriptor") {
    inputDir.set(file("src/main/assets"))
    outputFile.set(file("src/main/assets/${dataDescriptorName}"))
    dependsOn(installFcitxComponent)
    dependsOn(tasks.findByName("generateLicenseJson"))
}
/**
 * Note *Graph*
 * Tasks registered by [installFcitxComponent] implicitly depend .cxx dir to install generated files.
 * Since the native task `buildCMake$Variant$ABI` depend on the current variant and ABI,
 * we should have registered [installFcitxComponent] tasks for the cartesian product of $Variant and $ABI.
 * However, this would be way more tedious, as the build variant and ABI do not affect components we are going to install.
 * The essential cause of this situation is that it's impossible for gradle to handle dynamic dependencies,
 * where once the build graph was evaluated, no dependencies can be changed. So a trick is used here: when the task graph
 * is evaluated, we look into it to find out the name of the native task which will be executed, and then store its output
 * path in global variable. Tasks in [installFcitxComponent] are using the output path of the native task WITHOUT explicitly
 * depending on it.
 */
project.gradle.taskGraph.whenReady {
    val buildCMakeABITask = allTasks
        .find { it.name.startsWith("buildCMakeDebug[") || it.name.startsWith("buildCMakeRelWithDebInfo[") }
    if (buildCMakeABITask != null) {
        val cmakeDir = buildCMakeABITask.outputs.files.first().parentFile
        ext.set("cmakeDir", cmakeDir)
    }
}

android.applicationVariants.all {
    val variantName = name.capitalizeUS()
    tasks.findByName("merge${variantName}Assets")?.dependsOn(generateDataDescriptor)
    tasks.findByName("assemble${variantName}")?.dependsOn(generateBuildMetadata)
}

/**
 * DO NOT run these tasks manually. See Note *Graph* for details.
 */
fun installFcitxComponent(targetName: String, componentName: String, destDir: File) {
    // Deliberately use doLast to wait ext be set
    val build by tasks.register("buildFcitx${componentName.capitalizeUS()}") {
        doLast {
            try {
                exec {
                    workingDir = ext.get("cmakeDir") as File
                    commandLine("cmake", "--build", ".", "--target", targetName)
                }
            } catch (e: Exception) {
                logger.log(LogLevel.ERROR, "Failed to build target $targetName: ${e.message}")
                logger.log(LogLevel.ERROR, "Did you run this task independently?")
                throw e
            }
        }

        // make sure that this task runs after than the native task
        mustRunAfter("buildCMakeDebug[$buildABI]")
        mustRunAfter("buildCMakeRelWithDebInfo[$buildABI]")
    }

    val install by tasks.register("installFcitx${componentName.capitalizeUS()}") {
        doLast {
            try {
                exec {
                    environment("DESTDIR", destDir.absolutePath)
                    workingDir = ext.get("cmakeDir") as File
                    commandLine("cmake", "--install", ".", "--component", componentName)
                }
            } catch (e: Exception) {
                logger.log(
                    LogLevel.ERROR,
                    "Failed to install component $componentName: ${e.message}"
                )
                logger.log(LogLevel.ERROR, "Did you run this task independently?")
                throw e
            }
        }

        dependsOn(build)
    }

    installFcitxComponent.dependsOn(install)
}

installFcitxComponent("generate-desktop-file", "config", file("src/main/assets"))
installFcitxComponent("translation-file", "translation", file("src/main/assets"))

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register<Delete>("cleanGeneratedAssets") {
    delete(file("src/main/assets/usr/share/locale"))
    // delete all non symlink dirs
    delete(file("src/main/assets/usr/share/fcitx5").listFiles()?.filter {
        // https://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links
        File(it.parentFile.canonicalFile, it.name).let { s ->
            s.canonicalFile == s.absoluteFile
        }
    })
    delete(file("src/main/assets/${dataDescriptorName}"))
    delete(file("src/main/assets/licenses.json"))
}.also { tasks.clean.dependsOn(it) }

tasks.register<Delete>("cleanCxxIntermediates") {
    delete(file(".cxx"))
}.also { tasks.clean.dependsOn(it) }

dependencies {
    implementation("org.ini4j:ini4j:0.5.4")
    ksp(project(":codegen"))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.0")
    implementation("io.arrow-kt:arrow-core:1.1.5")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.github.CanHub:Android-Image-Cropper:4.2.1")
    implementation("cat.ereza:customactivityoncrash:2.4.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("org.mechdancer:dependency:0.1.2")
    val roomVersion = "2.5.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    val navVersion = "2.5.3"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    val splittiesVersion = "3.0.0"
    implementation("com.louiscad.splitties:splitties-bitflags:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-systemservices:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-views-dsl:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-views-dsl-constraintlayout:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-views-dsl-recyclerview:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-views-recyclerview:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-views-dsl-material:$splittiesVersion")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.5.1")
    androidTestImplementation("junit:junit:4.13.2")
}

abstract class DataDescriptorTask : DefaultTask() {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private val file by lazy { outputFile.get().asFile }

    private fun serialize(map: Map<String, String>) {
        file.deleteOnExit()
        file.writeText(
            JsonOutput.prettyPrint(
                JsonOutput.toJson(
                    mapOf<Any, Any>(
                        "sha256" to Hashing.sha256()
                            .hashString(
                                map.entries.joinToString { it.key + it.value },
                                Charset.defaultCharset()
                            )
                            .toString(),
                        "files" to map
                    )
                )
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserialize(): Map<String, String> =
        ((JsonSlurper().parseText(file.readText()) as Map<Any, Any>))["files"] as Map<String, String>

    companion object {
        fun sha256(file: File): String =
            Files.asByteSource(file).hash(Hashing.sha256()).toString()
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val map =
            file.exists()
                .takeIf { it }
                ?.runCatching {
                    deserialize()
                        // remove all old dirs
                        .filterValues { it.isNotBlank() }
                        .toMutableMap()
                }
                ?.getOrNull()
                ?: mutableMapOf()

        fun File.allParents(): List<File> =
            if (parentFile == null || parentFile.path in map)
                listOf()
            else
                listOf(parentFile) + parentFile.allParents()
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.file.name == file.name)
                return@forEach
            logger.log(LogLevel.DEBUG, "${change.changeType}: ${change.normalizedPath}")
            val relativeFile = change.file.relativeTo(file.parentFile)
            val key = relativeFile.path
            if (change.changeType == ChangeType.REMOVED) {
                map.remove(key)
            } else {
                map[key] = sha256(change.file)
            }
        }
        // calculate dirs
        inputDir.asFileTree.forEach {
            it.relativeTo(file.parentFile).allParents().forEach { p ->
                map[p.path] = ""
            }
        }
        serialize(map.toSortedMap())
    }
}
