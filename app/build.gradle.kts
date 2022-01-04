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

val gitRevCount = exec("git rev-list --count HEAD")
val gitHashShort = exec("git describe --always --dirty")
val gitVersionName = exec("git describe --tags --long --always --dirty")

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android")
}

val assetDescriptorName = "descriptor.json"

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    ndkVersion = System.getenv("NDK_VERSION") ?: "23.1.7779620"

    defaultConfig {
        applicationId = "me.rocka.fcitx5test"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "0.0.1"
        setProperty("archivesBaseName", "$applicationId-v$versionName-$gitRevCount-g$gitHashShort")
        buildConfigField("String", "BUILD_GIT_HASH", "\"$gitHashShort\"")
        buildConfigField("long", "BUILD_TIME", System.currentTimeMillis().toString())
        buildConfigField("String", "ASSETS_DESCRIPTOR_NAME", "\"${assetDescriptorName}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            version = "3.18.1"
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

val generateAssetDescriptor = tasks.register<AssetDescriptorTask>("generateAssetDescriptor") {
    inputDir.set(file("src/main/assets"))
    outputFile.set(file("src/main/assets/${assetDescriptorName}"))

}.also { tasks.preBuild.dependsOn(it) }


listOf("fcitx5", "fcitx5-chinese-addons").forEach {
    val task = tasks.register<MsgFmtTask>("MsgFmt-$it") {
        domain.set(it)
        inputDir.set(file("src/main/cpp/$it/po"))
        outputDir.set(file("src/main/assets/usr/share/locale"))
    }
    generateAssetDescriptor.dependsOn(task)
}


dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.lifecycle", "lifecycle-runtime-ktx", "2.3.1")
    implementation("androidx.lifecycle:lifecycle-service:2.4.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.4.2")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    val navVersion = "2.3.5"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("cn.berberman:girls-self-use:0.1.1")
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
    androidTestImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.4.0")
    androidTestImplementation("junit:junit:4.13.2")
}

abstract class MsgFmtTask : DefaultTask() {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val domain: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        inputChanges.getFileChanges(inputDir).forEach { change ->
            val fileName = change.normalizedPath
            if ((change.fileType == FileType.DIRECTORY) || (!fileName.endsWith(".po")))
                return@forEach
            println("${change.changeType}: $fileName")
            val locale = fileName.replace(".po", "")
            val targetDir = "$locale/LC_MESSAGES"
            outputDir.file(targetDir).get().asFile.mkdirs()
            val targetFile = outputDir.file("$targetDir/${domain.get()}.mo").get().asFile
            if (change.changeType == ChangeType.REMOVED) {
                targetFile.delete()
            } else {
                project.exec {
                    executable = "msgfmt"
                    args(change.file.absolutePath)
                    args("-o", targetFile.absolutePath)
                }
            }
        }
    }
}

abstract class AssetDescriptorTask : DefaultTask() {
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
        fun md5(file: File): String =
            Files.asByteSource(file).hash(Hashing.sha256()).toString()
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val map =
            file.exists()
                .takeIf { it }
                ?.runCatching { deserialize().toMutableMap() }
                ?.getOrNull()
                ?: mutableMapOf()
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY || change.file.name == file.name)
                return@forEach
            println("${change.changeType}: ${change.normalizedPath}")
            val key = change.file.relativeTo(file.parentFile).path
            if (change.changeType == ChangeType.REMOVED) {
                map.remove(key)
            } else {
                map[key] = md5(change.file)
            }
        }
        serialize(map)
    }
}