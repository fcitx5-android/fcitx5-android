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

val gitTag = exec("git describe --tags")
val gitRevCount = exec("git rev-list --count HEAD")
val gitHashShort = exec("git describe --always --dirty")
val gitVersionName = exec("git describe --tags --long --always")

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    id("com.cookpad.android.plugin.license-tools") version "1.2.0"
    kotlin("plugin.serialization") version "1.7.10"
    id("kotlin-parcelize")
}

val dataDescriptorName = "descriptor.json"

val targetABI = System.getenv("ABI")
    ?.takeIf { it.isNotBlank() }

// will be used if `targetABI` is unset
val defaultABI = "arm64-v8a"

android {
    namespace = "org.fcitx.fcitx5.android"
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    ndkVersion = System.getenv("NDK_VERSION") ?: "25.0.8775105"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android"
        minSdk = 23
        targetSdk = 31
        versionCode = 2
        versionName = gitVersionName
        setProperty("archivesBaseName", "$applicationId-$gitVersionName")
        buildConfigField("String", "BUILD_GIT_HASH", "\"$gitHashShort\"")
        buildConfigField("long", "BUILD_TIME", System.currentTimeMillis().toString())
        buildConfigField("String", "DATA_DESCRIPTOR_NAME", "\"${dataDescriptorName}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        if (targetABI == null)
            ndk {
                abiFilters.add(defaultABI)
            }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
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
            isDebuggable = true
            isDefault = true
            isJniDebuggable = true

            resValue("mipmap", "app_icon", "@mipmap/ic_launcher_debug")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round_debug")
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }

    splits {
        abi {
            targetABI?.let {
                isEnable = true
                reset()
                include(it)
            }
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

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
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
    val variantName = name.capitalize()
    tasks.findByName("merge${variantName}Assets")?.dependsOn(generateDataDescriptor)
}

/**
 * DO NOT run these tasks manually. See Note *Graph* for details.
 */
fun installFcitxComponent(targetName: String, componentName: String, destDir: File) {
    // Deliberately use doLast to wait ext be set
    val build by tasks.register("buildFcitx${componentName.capitalize()}") {
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
        targetABI?.let {
            mustRunAfter("buildCMakeDebug[$it]")
            mustRunAfter("buildCMakeRelWithDebInfo[$it]")
        } ?: run {
            mustRunAfter("buildCMakeDebug[$defaultABI]")
            mustRunAfter("buildCMakeRelWithDebInfo[$defaultABI]")
        }
    }

    tasks.register("installFcitx${componentName.capitalize()}") {
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
    }.also { installFcitxComponent.dependsOn(it) }
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.6")
    val arrowVersion = "1.1.2"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("com.github.CanHub:Android-Image-Cropper:4.2.1")
    implementation("cat.ereza:customactivityoncrash:2.4.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("org.mechdancer:dependency:0.1.2")
    val roomVersion = "2.4.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("net.java.dev.jna:jna:5.12.1@aar")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    val navVersion = "2.5.1"
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
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
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
