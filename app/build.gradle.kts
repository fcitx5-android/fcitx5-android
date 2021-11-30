import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream

fun exec(cmd: String): String = ByteArrayOutputStream().let {
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

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    ndkVersion = "23.1.7779620"

    defaultConfig {
        applicationId = "me.rocka.fcitx5test"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "0.0.1"
        setProperty("archivesBaseName", "$applicationId-v$versionName-$gitRevCount-g$gitHashShort")
        buildConfigField("String", "BUILD_GIT_HASH", "\"$gitHashShort\"")
        buildConfigField("long", "BUILD_TIME", System.currentTimeMillis().toString())
        // increase this value when update assets
        buildConfigField("long", "ASSETS_VERSION", "3")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

listOf("fcitx5", "fcitx5-chinese-addons").forEach {
    val taskName = "MsgFmt-$it"
    tasks.register<MsgFmtTask>(taskName) {
        domain.set(it)
        inputDir.set(file("src/main/cpp/$it/po"))
        outputDir.set(file("src/main/assets/fcitx5/locale"))
    }
    tasks.preBuild.dependsOn(taskName)
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
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("com.louiscad.splitties:splitties-views-dsl:3.0.0")
    implementation("com.louiscad.splitties:splitties-views-dsl-constraintlayout:3.0.0")
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
            if ((change.fileType == FileType.DIRECTORY) or (!fileName.endsWith(".po")))
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